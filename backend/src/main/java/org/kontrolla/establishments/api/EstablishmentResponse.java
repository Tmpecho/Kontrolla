package org.kontrolla.establishments.api;

import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.kontrolla.establishments.domain.EstablishmentType;

import java.time.Instant;
import java.util.UUID;

/**
 * API response describing an establishment.
 *
 * @param id the establishment identifier
 * @param organizationId the owning organization identifier
 * @param name the establishment name
 * @param type the establishment type
 * @param status the establishment status
 * @param createdAt when the establishment was created
 * @param updatedAt when the establishment was last updated
 */
public record EstablishmentResponse(
		UUID id,
		UUID organizationId,
		String name,
		EstablishmentType type,
		EstablishmentStatus status,
		Instant createdAt,
		Instant updatedAt
) {

	/**
	 * Maps an establishment entity to the API response shape.
	 *
	 * @param establishment the establishment to map
	 * @return the mapped response
	 */
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
