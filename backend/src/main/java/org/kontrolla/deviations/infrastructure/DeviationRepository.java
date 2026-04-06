package org.kontrolla.deviations.infrastructure;

import org.kontrolla.deviations.domain.Deviation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviationRepository extends JpaRepository<Deviation, UUID> {
	@EntityGraph(attributePaths = {"organization", "establishment", "createdByUser", "assignedToUser"})
	Page<Deviation> findByOrganizationId(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"organization", "establishment", "createdByUser", "assignedToUser"})
	Page<Deviation> findByEstablishmentIdAndOrganizationId(UUID establishmentId, UUID organizationId, Pageable pageable);

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
