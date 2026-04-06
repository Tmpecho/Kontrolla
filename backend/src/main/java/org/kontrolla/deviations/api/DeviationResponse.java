package org.kontrolla.deviations.api;

import org.kontrolla.deviations.domain.Deviation;
import org.kontrolla.deviations.domain.DeviationCategory;
import org.kontrolla.deviations.domain.DeviationSeverity;
import org.kontrolla.deviations.domain.DeviationStatus;

import java.time.Instant;
import java.util.UUID;

public record DeviationResponse(
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
		Instant updatedAt
) {

	public static DeviationResponse from(Deviation deviation) {
		return new DeviationResponse(
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
				deviation.getUpdatedAt()
		);
	}
}
