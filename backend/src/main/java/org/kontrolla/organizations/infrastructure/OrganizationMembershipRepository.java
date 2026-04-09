package org.kontrolla.organizations.infrastructure;

import org.kontrolla.organizations.domain.OrganizationMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	Page<OrganizationMembership> findByOrganizationId(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	Page<OrganizationMembership> findByOrganizationIdAndActiveTrue(UUID organizationId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	@Query(
			value = """
					select distinct membership
					from OrganizationMembership membership
					left join membership.accessibleEstablishments accessibleEstablishment
					where membership.organization.id = :organizationId
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = :establishmentId)
					""",
			countQuery = """
					select count(distinct membership.id)
					from OrganizationMembership membership
					left join membership.accessibleEstablishments accessibleEstablishment
					where membership.organization.id = :organizationId
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = :establishmentId)
					"""
	)
	Page<OrganizationMembership> findByOrganizationIdAndAccessibleEstablishmentId(
			@Param("organizationId") UUID organizationId,
			@Param("establishmentId") UUID establishmentId,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	@Query(
			value = """
					select distinct membership
					from OrganizationMembership membership
					left join membership.accessibleEstablishments accessibleEstablishment
					where membership.organization.id = :organizationId
					and membership.active = true
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = :establishmentId)
					""",
			countQuery = """
					select count(distinct membership.id)
					from OrganizationMembership membership
					left join membership.accessibleEstablishments accessibleEstablishment
					where membership.organization.id = :organizationId
					and membership.active = true
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = :establishmentId)
					"""
	)
	Page<OrganizationMembership> findByOrganizationIdAndActiveTrueAndAccessibleEstablishmentId(
			@Param("organizationId") UUID organizationId,
			@Param("establishmentId") UUID establishmentId,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);

	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findFirstByUserIdAndActiveTrueOrderByCreatedAtAsc(UUID userId);
}
