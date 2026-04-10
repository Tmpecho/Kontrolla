package org.kontrolla.notifications.application;

import java.util.UUID;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationServiceArea;
import org.kontrolla.notifications.domain.NotificationType;

/**
 * Command for creating a notification for a user.
 *
 * @param recipientUserId the user who should receive the notification
 * @param actorUserId the user who triggered the notification, if any
 * @param organizationId the organization the notification belongs to
 * @param establishmentId the establishment the notification belongs to
 * @param serviceArea the functional area associated with the notification
 * @param type the notification type
 * @param title the notification title
 * @param message the notification message
 * @param resourceType the type of related resource
 * @param resourceId the identifier of the related resource
 */
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
    UUID resourceId) {}
