package org.kontrolla.audit.application;

import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AuditRecord {

	private final AuditAction action;
	private final AuditOutcome outcome;
	private final String resultCode;
	private final AuditActorType actorType;
	private final UUID actorUserId;
	private final String actorEmail;
	private final UUID organizationId;
	private final AuditTargetType targetType;
	private final UUID targetId;
	private final Map<String, Object> metadata;

	private AuditRecord(Builder builder) {
		this.action = builder.action;
		this.outcome = builder.outcome;
		this.resultCode = builder.resultCode;
		this.actorType = builder.actorType;
		this.actorUserId = builder.actorUserId;
		this.actorEmail = builder.actorEmail;
		this.organizationId = builder.organizationId;
		this.targetType = builder.targetType;
		this.targetId = builder.targetId;
		this.metadata = Map.copyOf(builder.metadata);
	}

	public static Builder builder(AuditAction action, AuditOutcome outcome, String resultCode) {
		return new Builder(action, outcome, resultCode);
	}

	public AuditAction getAction() {
		return action;
	}

	public AuditOutcome getOutcome() {
		return outcome;
	}

	public String getResultCode() {
		return resultCode;
	}

	public AuditActorType getActorType() {
		return actorType;
	}

	public UUID getActorUserId() {
		return actorUserId;
	}

	public String getActorEmail() {
		return actorEmail;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public AuditTargetType getTargetType() {
		return targetType;
	}

	public UUID getTargetId() {
		return targetId;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public static final class Builder {

		private final AuditAction action;
		private final AuditOutcome outcome;
		private final String resultCode;
		private AuditActorType actorType;
		private UUID actorUserId;
		private String actorEmail;
		private UUID organizationId;
		private AuditTargetType targetType;
		private UUID targetId;
		private final LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();

		private Builder(AuditAction action, AuditOutcome outcome, String resultCode) {
			this.action = action;
			this.outcome = outcome;
			this.resultCode = resultCode;
		}

		public Builder actor(AuditActorType actorType, UUID actorUserId, String actorEmail) {
			this.actorType = actorType;
			this.actorUserId = actorUserId;
			this.actorEmail = actorEmail;
			return this;
		}

		public Builder organizationId(UUID organizationId) {
			this.organizationId = organizationId;
			return this;
		}

		public Builder target(AuditTargetType targetType, UUID targetId) {
			this.targetType = targetType;
			this.targetId = targetId;
			return this;
		}

		public Builder metadata(String key, Object value) {
			if (value != null) {
				metadata.put(key, value);
			}
			return this;
		}

		public Builder metadata(Map<String, ?> values) {
			values.forEach(this::metadata);
			return this;
		}

		public AuditRecord build() {
			return new AuditRecord(this);
		}
	}
}
