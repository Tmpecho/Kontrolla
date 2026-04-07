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
