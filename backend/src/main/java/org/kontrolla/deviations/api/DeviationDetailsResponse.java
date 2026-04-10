package org.kontrolla.deviations.api;

import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationEvent;
import org.kontrolla.deviations.domain.DeviationEventType;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;
import org.kontrolla.iam.domain.User;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * API response describing a deviation together with its timeline.
 *
 * @param id the deviation identifier
 * @param organizationId the owning organization identifier
 * @param establishmentId the owning establishment identifier
 * @param createdByUserId the creator user identifier
 * @param assignedToUserId the assigned user identifier, if any
 * @param title the deviation title
 * @param description the deviation description
 * @param status the deviation status
 * @param severity the deviation severity
 * @param category the deviation category
 * @param createdAt when the deviation was created
 * @param updatedAt when the deviation was last updated
 * @param timeline the ordered deviation timeline entries
 */
public record DeviationDetailsResponse(
		UUID id,
		UUID organizationId,
		UUID establishmentId,
		UUID createdByUserId,
		UUID assignedToUserId,
		String title,
		String description,
		DeviationStatus status,
		DeviationSeverity severity,
		DeviationCategory category,
		Instant createdAt,
		Instant updatedAt,
		List<DeviationTimelineEntryResponse> timeline
) {

	/**
	 * Maps a deviation entity to the detailed API response shape.
	 *
	 * @param deviation the deviation to map
	 * @return the mapped response
	 */
	public static DeviationDetailsResponse from(Deviation deviation) {
		return new DeviationDetailsResponse(
				deviation.getId(),
				deviation.getOrganization().getId(),
				deviation.getEstablishment().getId(),
				deviation.getCreatedByUser().getId(),
				deviation.getAssignedToUser() == null ? null : deviation.getAssignedToUser().getId(),
				deviation.getTitle(),
				deviation.getDescription(),
				deviation.getStatus(),
				deviation.getSeverity(),
				deviation.getCategory(),
				deviation.getCreatedAt(),
				deviation.getUpdatedAt(),
				deviation.getEvents().stream()
						.sorted(Comparator.comparing(DeviationEvent::getOccurredAt))
						.map(DeviationTimelineEntryResponse::from)
						.toList()
		);
	}

	/**
	 * API response describing a single deviation timeline entry.
	 *
	 * @param id the event identifier
	 * @param eventType the event type
	 * @param actorUserId the actor user identifier, if any
	 * @param authorName the display name of the event author
	 * @param note the timeline note
	 * @param occurredAt when the event occurred
	 */
	public record DeviationTimelineEntryResponse(
			UUID id,
			DeviationEventType eventType,
			UUID actorUserId,
			String authorName,
			String note,
			Instant occurredAt
	) {

		private static DeviationTimelineEntryResponse from(DeviationEvent event) {
			return new DeviationTimelineEntryResponse(
					event.getId(),
					event.getEventType(),
					event.getActorUser() == null ? null : event.getActorUser().getId(),
					buildAuthorName(event.getActorUser()),
					event.getNote(),
					event.getOccurredAt()
			);
		}

		private static String buildAuthorName(User actorUser) {
			if (actorUser == null) {
				return "System";
			}

			String fullName = (actorUser.getFirstName() + " " + actorUser.getLastName()).trim();
			return fullName.isBlank() ? actorUser.getEmail() : fullName;
		}
	}
}
