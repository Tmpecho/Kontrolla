package org.kontrolla.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persisted audit event containing the resolved context of a security- or business-relevant action.
 */
@Getter
@Entity
@Table(name = "audit_events")
public class AuditEvent {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, updatable = false, length = 36)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private AuditAction action;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private AuditOutcome outcome;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "actor_type", nullable = false, length = 32)
  private AuditActorType actorType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "actor_user_id", length = 36)
  private UUID actorUserId;

  @Column(name = "actor_email", length = 320)
  private String actorEmail;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "organization_id", length = 36)
  private UUID organizationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", length = 32)
  private AuditTargetType targetType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "target_id", length = 36)
  private UUID targetId;

  @Column(name = "request_method", length = 16)
  private String requestMethod;

  @Column(name = "request_path", length = 255)
  private String requestPath;

  @Column(name = "client_ip", length = 64)
  private String clientIp;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "result_code", nullable = false, length = 64)
  private String resultCode;

  @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
  @Column(name = "metadata_json", nullable = false, columnDefinition = "LONGTEXT")
  private String metadataJson;

  protected AuditEvent() {}

  /**
   * Creates an audit event ready for persistence.
   *
   * @param action the audited action
   * @param outcome the outcome of the action
   * @param occurredAt when the action occurred
   * @param actorType the type of actor responsible for the action
   * @param actorUserId the acting user id, if available
   * @param actorEmail the acting user email, if available
   * @param organizationId the organization associated with the event, if any
   * @param targetType the type of target resource, if any
   * @param targetId the target resource id, if any
   * @param requestMethod the HTTP request method, if available
   * @param requestPath the HTTP request path, if available
   * @param clientIp the client IP address, if available
   * @param userAgent the user agent string, if available
   * @param resultCode the application result code
   * @param metadataJson the serialized metadata payload
   */
  public AuditEvent(
      AuditAction action,
      AuditOutcome outcome,
      Instant occurredAt,
      AuditActorType actorType,
      UUID actorUserId,
      String actorEmail,
      UUID organizationId,
      AuditTargetType targetType,
      UUID targetId,
      String requestMethod,
      String requestPath,
      String clientIp,
      String userAgent,
      String resultCode,
      String metadataJson) {
    this.action = action;
    this.outcome = outcome;
    this.occurredAt = occurredAt;
    this.actorType = actorType;
    this.actorUserId = actorUserId;
    this.actorEmail = actorEmail;
    this.organizationId = organizationId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.requestMethod = requestMethod;
    this.requestPath = requestPath;
    this.clientIp = clientIp;
    this.userAgent = userAgent;
    this.resultCode = resultCode;
    this.metadataJson = metadataJson;
  }

  @PrePersist
  protected void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (occurredAt == null) {
      occurredAt = Instant.now();
    }
  }
}
