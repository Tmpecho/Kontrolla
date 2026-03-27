package org.kontrolla.establishments.infrastructure;

import org.kontrolla.establishments.domain.Establishment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {

	@EntityGraph(attributePaths = {"organization"})
	Page<Establishment> findByOrganizationId(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findByIdAndOrganizationId(UUID id, UUID organizationId);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);
}
