package org.kontrolla.notifications.api;

import org.kontrolla.common.api.PageResponse;
import org.kontrolla.iam.security.CurrentUser;
import org.kontrolla.notifications.application.NotificationService;
import org.kontrolla.notifications.domain.NotificationStatusFilter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST API for reading and updating notifications for the current user.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * Creates a controller backed by the notification service.
	 *
	 * @param notificationService service handling notification operations
	 */
	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	/**
	 * Returns a paginated list of notifications for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @param status the requested read-status filter
	 * @param pageable pagination information
	 * @return the page of notification responses
	 */
	@GetMapping
	public PageResponse<NotificationResponse> listNotifications(
			@AuthenticationPrincipal CurrentUser currentUser,
			@RequestParam(defaultValue = "ALL") NotificationStatusFilter status,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return PageResponse.from(
				notificationService.listNotifications(currentUser, status, pageable),
				NotificationResponse::from
		);
	}

	/**
	 * Returns the unread notification count for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @return the unread notification count response
	 */
	@GetMapping("/unread-count")
	public UnreadNotificationCountResponse unreadCount(@AuthenticationPrincipal CurrentUser currentUser) {
		return new UnreadNotificationCountResponse(notificationService.getUnreadCount(currentUser));
	}

	/**
	 * Marks a single notification as read.
	 *
	 * @param notificationId the notification identifier
	 * @param currentUser the authenticated user
	 * @return the updated notification response
	 */
	@PostMapping("/{notificationId}/read")
	public NotificationResponse markRead(
			@PathVariable UUID notificationId,
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		return NotificationResponse.from(notificationService.markRead(notificationId, currentUser));
	}

	/**
	 * Marks all unread notifications as read for the current user.
	 *
	 * @param currentUser the authenticated user
	 * @return the unread notification count response after the update
	 */
	@PostMapping("/read-all")
	public UnreadNotificationCountResponse markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
		return new UnreadNotificationCountResponse(notificationService.markAllRead(currentUser));
	}
}
