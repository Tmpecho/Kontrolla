package org.kontrolla.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted notification delivered to a user and optionally marked as read.
 */
@Getter
@Entity
@Table(name = "notifications")
public class Notification extends AbstractAuditableUuidEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "recipient_user_id", nullable = false, updatable = false, length = 36)
	private UUID recipientUserId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "organization_id", nullable = false, updatable = false, length = 36)
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "establishment_id", nullable = false, updatable = false, length = 36)
	private UUID establishmentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "service_area", nullable = false, updatable = false, length = 32)
	private NotificationServiceArea serviceArea;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 64)
	private NotificationType type;

	@Column(nullable = false, updatable = false)
	private String title;

	@Column(nullable = false, updatable = false, length = 2000)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "resource_type", nullable = false, updatable = false, length = 32)
	private NotificationResourceType resourceType;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "resource_id", nullable = false, updatable = false, length = 36)
	private UUID resourceId;

	@Column(name = "read_at")
	private Instant readAt;

	protected Notification() {
	}

	/**
	 * Creates a notification for a recipient.
	 *
	 * @param recipientUserId the recipient user identifier
	 * @param organizationId the related organization identifier
	 * @param establishmentId the related establishment identifier
	 * @param serviceArea the functional service area
	 * @param type the notification type
	 * @param title the notification title
	 * @param message the notification message
	 * @param resourceType the related resource type
	 * @param resourceId the related resource identifier
	 */
	public Notification(
			UUID recipientUserId,
			UUID organizationId,
			UUID establishmentId,
			NotificationServiceArea serviceArea,
			NotificationType type,
			String title,
			String message,
			NotificationResourceType resourceType,
			UUID resourceId
	) {
		this.recipientUserId = recipientUserId;
		this.organizationId = organizationId;
		this.establishmentId = establishmentId;
		this.serviceArea = serviceArea;
		this.type = type;
		this.title = title;
		this.message = message;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
	}

	/**
	 * Marks the notification as read if it has not already been marked.
	 *
	 * @param readAt the time the notification was read
	 */
	public void markRead(Instant readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}
}
