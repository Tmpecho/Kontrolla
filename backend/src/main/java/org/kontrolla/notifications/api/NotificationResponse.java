package org.kontrolla.notifications.api;

import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.notifications.domain.Notification;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
		UUID id,
		UUID recipientUserId,
		UUID organizationId,
		UUID establishmentId,
		ChecklistServiceArea serviceArea,
		NotificationType type,
		String title,
		String message,
		NotificationResourceType resourceType,
		UUID resourceId,
		Instant createdAt,
		Instant readAt
) {

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
