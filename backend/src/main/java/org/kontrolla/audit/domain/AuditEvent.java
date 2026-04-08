package org.kontrolla.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

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

	@Lob
	@Column(name = "metadata_json", nullable = false)
	private String metadataJson;

	protected AuditEvent() {
	}

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
			String metadataJson
	) {
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
