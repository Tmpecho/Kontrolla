package org.kontrolla.audit.application;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.iam.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Enriches audit records with request and security-context information captured from the current
 * thread.
 */
@Component
public class AuditRequestContextResolver {

  private final Clock clock;

  /**
   * Creates a resolver that timestamps audit events using the shared application clock.
   *
   * @param clock the clock used when resolving audit event timestamps
   */
  public AuditRequestContextResolver(Clock clock) {
    this.clock = clock;
  }

  /**
   * Resolves a raw audit record into a fully populated record ready for persistence.
   *
   * @param auditRecord the source audit record
   * @return the resolved audit record with contextual request information
   */
  public ResolvedAuditRecord resolve(AuditRecord auditRecord) {
    HttpServletRequest request = currentRequest();
    CurrentUser currentUser = currentUser();

    AuditActorType actorType = auditRecord.getActorType();
    if (actorType == null) {
      actorType = currentUser == null ? AuditActorType.ANONYMOUS : AuditActorType.USER;
    }

    return new ResolvedAuditRecord(
        auditRecord.getAction(),
        auditRecord.getOutcome(),
        Instant.now(clock),
        actorType,
        auditRecord.getActorUserId() != null
            ? auditRecord.getActorUserId()
            : currentUser == null ? null : currentUser.userId(),
        sanitize(
            auditRecord.getActorEmail(), 320, currentUser == null ? null : currentUser.email()),
        auditRecord.getOrganizationId(),
        auditRecord.getTargetType(),
        auditRecord.getTargetId(),
        sanitize(request == null ? null : request.getMethod(), 16, null),
        sanitize(request == null ? null : request.getRequestURI(), 255, null),
        sanitize(request == null ? null : request.getRemoteAddr(), 64, null),
        sanitize(request == null ? null : request.getHeader("User-Agent"), 512, null),
        sanitize(auditRecord.getResultCode(), 64, null),
        auditRecord.getMetadata());
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
      return servletRequestAttributes.getRequest();
    }
    return null;
  }

  private CurrentUser currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    return principal instanceof CurrentUser currentUser ? currentUser : null;
  }

  private String sanitize(String value, int maxLength, String fallbackValue) {
    String resolvedValue = value == null || value.isBlank() ? fallbackValue : value;
    if (resolvedValue == null) {
      return null;
    }
    String trimmedValue = resolvedValue.strip();
    return trimmedValue.length() <= maxLength ? trimmedValue : trimmedValue.substring(0, maxLength);
  }
}
