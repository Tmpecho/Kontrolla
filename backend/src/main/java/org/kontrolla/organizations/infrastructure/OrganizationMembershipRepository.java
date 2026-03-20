package org.kontrolla.organizations.infrastructure;

import org.kontrolla.organizations.domain.OrganizationMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

	@EntityGraph(attributePaths = {"user"})
	Page<OrganizationMembership> findByOrganizationId(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"organization", "user"})
	Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	@EntityGraph(attributePaths = {"organization", "user"})
	Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
