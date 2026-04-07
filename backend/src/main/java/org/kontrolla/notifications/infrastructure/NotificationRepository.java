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

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

	@Query("""
			select n
			from Notification n
			where n.recipientUserId = :recipientUserId
			  and (:unreadOnly = false or n.readAt is null)
			order by case when n.readAt is null then 0 else 1 end, n.createdAt desc
			""")
	Page<Notification> findPageForRecipient(@Param("recipientUserId") UUID recipientUserId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

	Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

	long countByRecipientUserIdAndReadAtIsNull(UUID recipientUserId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update Notification n
			set n.readAt = :readAt
			where n.recipientUserId = :recipientUserId
			  and n.readAt is null
			""")
	int markAllRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") Instant readAt);
}
