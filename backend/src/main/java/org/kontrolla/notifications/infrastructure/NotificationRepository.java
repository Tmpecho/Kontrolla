package org.kontrolla.notifications.infrastructure;

import org.kontrolla.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for notification persistence and recipient-specific queries.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	/**
	 * Returns a page of notifications for a recipient, optionally restricted to
	 * unread notifications.
	 *
	 * @param recipientUserId the recipient user identifier
	 * @param unreadOnly whether only unread notifications should be returned
	 * @param pageable pagination information
	 * @return the matching notification page
	 */
	@Query("""
			select n
			from Notification n
			where n.recipientUserId = :recipientUserId
			  and (:unreadOnly = false or n.readAt is null)
			order by case when n.readAt is null then 0 else 1 end, n.createdAt desc
			""")
	Page<Notification> findPageForRecipient(@Param("recipientUserId") UUID recipientUserId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

	/**
	 * Finds a notification by id scoped to a specific recipient.
	 *
	 * @param id the notification identifier
	 * @param recipientUserId the recipient user identifier
	 * @return the matching notification, if present
	 */
	Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

	/**
	 * Counts unread notifications for a recipient.
	 *
	 * @param recipientUserId the recipient user identifier
	 * @return the unread notification count
	 */
	long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

	/**
	 * Marks all unread notifications as read for a recipient.
	 *
	 * @param recipientUserId the recipient user identifier
	 * @param readAt the timestamp to set on updated notifications
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Notification n
			set n.readAt = :readAt
			where n.recipientUserId = :recipientUserId
			  and n.readAt is null
			""")
	void markAllRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") Instant readAt);
}
