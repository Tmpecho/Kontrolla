package org.kontrolla.audit.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.kontrolla.audit.domain.AuditAction;
import org.kontrolla.audit.domain.AuditActorType;
import org.kontrolla.audit.domain.AuditOutcome;
import org.kontrolla.audit.domain.AuditTargetType;

/**
 * Immutable description of an audit event before request and security context information is
 * resolved.
 */
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

  /**
   * Creates a builder for a new audit record.
   *
   * @param action the audited action
   * @param outcome the outcome of the action
   * @param resultCode the application result code associated with the event
   * @return a builder for the audit record
   */
  public static Builder builder(AuditAction action, AuditOutcome outcome, String resultCode) {
    return new Builder(action, outcome, resultCode);
  }

  /**
   * Returns the audited action.
   *
   * @return the audit action
   */
  public AuditAction getAction() {
    return action;
  }

  /**
   * Returns the outcome of the audited action.
   *
   * @return the audit outcome
   */
  public AuditOutcome getOutcome() {
    return outcome;
  }

  /**
   * Returns the application result code associated with the audit event.
   *
   * @return the result code
   */
  public String getResultCode() {
    return resultCode;
  }

  /**
   * Returns the actor type recorded explicitly on the event, if any.
   *
   * @return the actor type, or {@code null} when it should be resolved later
   */
  public AuditActorType getActorType() {
    return actorType;
  }

  /**
   * Returns the user id of the actor, if explicitly provided.
   *
   * @return the actor user id, or {@code null}
   */
  public UUID getActorUserId() {
    return actorUserId;
  }

  /**
   * Returns the actor email address, if explicitly provided.
   *
   * @return the actor email, or {@code null}
   */
  public String getActorEmail() {
    return actorEmail;
  }

  /**
   * Returns the organization associated with the event, if any.
   *
   * @return the organization id, or {@code null}
   */
  public UUID getOrganizationId() {
    return organizationId;
  }

  /**
   * Returns the type of target entity associated with the event.
   *
   * @return the target type, or {@code null}
   */
  public AuditTargetType getTargetType() {
    return targetType;
  }

  /**
   * Returns the identifier of the target entity associated with the event.
   *
   * @return the target id, or {@code null}
   */
  public UUID getTargetId() {
    return targetId;
  }

  /**
   * Returns event metadata that should be persisted and logged with the audit entry.
   *
   * @return an immutable metadata map
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /** Builder for composing audit records with optional actor, target, and metadata fields. */
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

    /**
     * Sets actor details for the audit event.
     *
     * @param actorType the actor type to record
     * @param actorUserId the actor user id, if available
     * @param actorEmail the actor email address, if available
     * @return this builder
     */
    public Builder actor(AuditActorType actorType, UUID actorUserId, String actorEmail) {
      this.actorType = actorType;
      this.actorUserId = actorUserId;
      this.actorEmail = actorEmail;
      return this;
    }

    /**
     * Associates the audit event with an organization.
     *
     * @param organizationId the organization id
     * @return this builder
     */
    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    /**
     * Sets the target resource for the audit event.
     *
     * @param targetType the target type
     * @param targetId the target identifier
     * @return this builder
     */
    public Builder target(AuditTargetType targetType, UUID targetId) {
      this.targetType = targetType;
      this.targetId = targetId;
      return this;
    }

    /**
     * Adds a metadata entry when the value is non-null.
     *
     * @param key the metadata key
     * @param value the metadata value
     * @return this builder
     */
    public Builder metadata(String key, Object value) {
      if (value != null) {
        metadata.put(key, value);
      }
      return this;
    }

    /**
     * Adds all metadata entries from the provided map, skipping null values.
     *
     * @param values the metadata values to add
     * @return this builder
     */
    public Builder metadata(Map<String, ?> values) {
      values.forEach(this::metadata);
      return this;
    }

    /**
     * Builds the immutable audit record.
     *
     * @return the completed audit record
     */
    public AuditRecord build() {
      return new AuditRecord(this);
    }
  }
}
