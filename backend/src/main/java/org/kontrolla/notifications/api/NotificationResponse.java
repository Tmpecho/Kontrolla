package org.kontrolla.notifications.api;

import org.kontrolla.notifications.domain.Notification;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationServiceArea;
import org.kontrolla.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * API response describing a notification delivered to a user.
 *
 * @param id the notification identifier
 * @param recipientUserId the recipient user identifier
 * @param organizationId the related organization identifier
 * @param establishmentId the related establishment identifier
 * @param serviceArea the related service area
 * @param type the notification type
 * @param title the notification title
 * @param message the notification message
 * @param resourceType the related resource type
 * @param resourceId the related resource identifier
 * @param createdAt when the notification was created
 * @param readAt when the notification was read, if applicable
 */
public record NotificationResponse(
		UUID id,
		UUID recipientUserId,
		UUID organizationId,
		UUID establishmentId,
		NotificationServiceArea serviceArea,
		NotificationType type,
		String title,
		String message,
		NotificationResourceType resourceType,
		UUID resourceId,
		Instant createdAt,
		Instant readAt
) {

	/**
	 * Maps a notification entity to the API response shape.
	 *
	 * @param notification the notification to map
	 * @return the mapped response
	 */
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getRecipientUserId(),
				notification.getOrganizationId(),
				notification.getEstablishmentId(),
				notification.getServiceArea(),
				notification.getType(),
				notification.getTitle(),
				notification.getMessage(),
				notification.getResourceType(),
				notification.getResourceId(),
				notification.getCreatedAt(),
				notification.getReadAt()
		);
	}
}
