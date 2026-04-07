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

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;

	public NotificationService(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

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

	@Transactional(readOnly = true)
	public long getUnreadCount(CurrentUser currentUser) {
		return notificationRepository.countByRecipientUserIdAndReadAtIsNull(currentUser.userId());
	}

	@Transactional
	public Notification markRead(UUID notificationId, CurrentUser currentUser) {
		Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, currentUser.userId())
				.orElseThrow(() -> new ResourceNotFoundException("notification_not_found", "Notification not found"));
		notification.markRead(Instant.now());
		return notification;
	}

	@Transactional
	public long markAllRead(CurrentUser currentUser) {
		notificationRepository.markAllRead(currentUser.userId(), Instant.now());
		return 0;
	}

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
