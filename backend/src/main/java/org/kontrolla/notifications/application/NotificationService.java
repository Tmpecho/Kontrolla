package org.kontrolla.notifications.application;

import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.common.exception.ResourceNotFoundException;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.notifications.domain.Notification;
import org.kontrolla.notifications.domain.NotificationResourceType;
import org.kontrolla.notifications.domain.NotificationStatusFilter;
import org.kontrolla.notifications.domain.NotificationType;
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
	public void markAllRead(CurrentUser currentUser) {
		notificationRepository.markAllRead(currentUser.userId(), Instant.now());
	}

	@Transactional
	public void createNotification(
			UUID recipientUserId,
			UUID actorUserId,
			UUID organizationId,
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			NotificationType type,
			String title,
			String message,
			NotificationResourceType resourceType,
			UUID resourceId
	) {
		if (recipientUserId == null || recipientUserId.equals(actorUserId)) {
			return;
		}

		notificationRepository.save(new Notification(
				recipientUserId,
				organizationId,
				establishmentId,
				serviceArea,
				type,
				title,
				message,
				resourceType,
				resourceId
		));
	}
}
