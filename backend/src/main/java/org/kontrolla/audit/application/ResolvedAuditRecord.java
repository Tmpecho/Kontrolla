package org.kontrolla.audit.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;

record ResolvedAuditRecord(
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
    Map<String, Object> metadata) {}
