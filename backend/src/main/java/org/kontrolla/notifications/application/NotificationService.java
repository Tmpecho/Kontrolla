package org.kontrolla.notifications.application;

import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.notifications.domain.Notification;
import org.kontrolla.notifications.domain.NotificationStatusFilter;
import org.kontrolla.notifications.infrastructure.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles notification retrieval, read-state updates, and notification
 * creation.
 */
@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	/**
	 * Creates a notification service backed by the notification repository.
	 *
	 * @param notificationRepository repository for notification persistence
	 */
	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	/**
	 * Returns a page of notifications for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @param status the requested read-status filter
	 * @param pageable pagination information
	 * @return a page of notifications
	 */
	@Transactional(readOnly = true)
	public Page<Notification> listNotifications(
			CurrentUser currentUser,
			NotificationStatusFilter status,
			Pageable pageable
	) {
		return notificationRepository.findPageForRecipient(
				currentUser.userId(),
				status == NotificationStatusFilter.UNREAD,
				pageable
		);
	}

	/**
	 * Returns the unread notification count for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @return the unread notification count
	 */
	@Transactional(readOnly = true)
	public long getUnreadCount(CurrentUser currentUser) {
		return notificationRepository.countByRecipientUserIdAndReadAtIsNull(currentUser.userId());
	}

	/**
	 * Marks a notification as read for the current user.
	 *
	 * @param notificationId the notification identifier
	 * @param currentUser the authenticated user
	 * @return the updated notification
	 */
	@Transactional
	public Notification markRead(UUID notificationId, CurrentUser currentUser) {
		Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, currentUser.userId())
				.orElseThrow(() -> new ResourceNotFoundException("notification_not_found", "Notification not found"));
		notification.markRead(Instant.now());
		return notification;
	}

	/**
	 * Marks all unread notifications as read for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @return the unread notification count after the update
	 */
	@Transactional
	public long markAllRead(CurrentUser currentUser) {
		notificationRepository.markAllRead(currentUser.userId(), Instant.now());
		return getUnreadCount(currentUser);
	}

	/**
	 * Creates a notification when the recipient is present and differs from the
	 * actor that triggered the event.
	 *
	 * @param command the notification creation command
	 */
	@Transactional
	public void createNotification(CreateNotificationCommand command) {
		if (command.recipientUserId() == null || command.recipientUserId().equals(command.actorUserId())) {
			return;
		}

		notificationRepository.save(new Notification(
				command.recipientUserId(),
				command.organizationId(),
				command.establishmentId(),
				command.serviceArea(),
				command.type(),
				command.title(),
				command.message(),
				command.resourceType(),
				command.resourceId()
		));
	}
}
