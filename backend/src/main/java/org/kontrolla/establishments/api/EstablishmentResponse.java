package org.kontrolla.establishments.api;

import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;

import java.time.Instant;
import java.util.UUID;

public record EstablishmentResponse(
		UUID id,
		UUID organizationId,
		String name,
		EstablishmentType type,
		EstablishmentStatus status,
		Instant createdAt,
		Instant updatedAt
) {

	public static EstablishmentResponse from(Establishment establishment) {
		return new EstablishmentResponse(
				establishment.getId(),
				establishment.getOrganization().getId(),
				establishment.getName(),
				establishment.getType(),
				establishment.getStatus(),
				establishment.getCreatedAt(),
				establishment.getUpdatedAt()
		);
	}
}
