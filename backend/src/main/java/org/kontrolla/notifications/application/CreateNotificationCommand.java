package org.kontrolla.notifications.application;

import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationServiceArea;
import org.kontrolla.notifications.domain.NotificationType;

import java.util.UUID;

public record CreateNotificationCommand(
		UUID recipientUserId,
		UUID actorUserId,
		UUID organizationId,
		UUID establishmentId,
		NotificationServiceArea serviceArea,
		NotificationType type,
		String title,
		String message,
		NotificationResourceType resourceType,
		UUID resourceId
) {
}
