package org.kontrolla.deviations.infrastructure;

import org.kontrolla.deviations.domain.Deviation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for deviation persistence and organization- or
 * establishment-scoped queries.
 */
public interface DeviationRepository extends JpaRepository<Deviation, UUID> {
	/**
	 * Checks whether a deviation with the same title already exists for an
	 * establishment.
	 *
	 * @param establishmentId the establishment identifier
	 * @param title the deviation title
	 * @return {@code true} when a matching deviation exists
	 */
	boolean existsByEstablishmentIdAndTitleIgnoreCase(UUID establishmentId, String title);

	/**
	 * Returns deviations for an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param pageable pagination information
	 * @return a page of deviations
	 */
	@EntityGraph(attributePaths = {"organization", "establishment", "createdByUser", "assignedToUser"})
	Page<Deviation> findByOrganizationId(UUID organizationId, Pageable pageable);

	/**
	 * Returns deviations for an establishment within an organization.
	 *
	 * @param establishmentId the establishment identifier
	 * @param organizationId the organization identifier
	 * @param pageable pagination information
	 * @return a page of deviations
	 */
	@EntityGraph(attributePaths = {"organization", "establishment", "createdByUser", "assignedToUser"})
	Page<Deviation> findByEstablishmentIdAndOrganizationId(UUID establishmentId, UUID organizationId, Pageable pageable);

	/**
	 * Finds a deviation by id scoped to an establishment and organization.
	 *
	 * @param id the deviation identifier
	 * @param establishmentId the establishment identifier
	 * @param organizationId the organization identifier
	 * @return the matching deviation, if present
	 */
	@EntityGraph(attributePaths = {
			"organization",
			"establishment",
			"createdByUser",
			"assignedToUser",
			"events",
			"events.actorUser"
	})
	Optional<Deviation> findByIdAndEstablishmentIdAndOrganizationId(UUID id, UUID establishmentId, UUID organizationId);
}
