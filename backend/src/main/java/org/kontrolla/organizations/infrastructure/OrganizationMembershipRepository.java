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

/**
 * Repository for organization membership queries and access-scope lookups.
 */
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {

	/**
	 * Returns memberships for an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param pageable pagination information
	 * @return a page of memberships
	 */
	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	Page<OrganizationMembership> findByOrganizationId(UUID organizationId, Pageable pageable);

	/**
	 * Returns active memberships for an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param pageable pagination information
	 * @return a page of active memberships
	 */
	@EntityGraph(attributePaths = {"user", "accessibleEstablishments"})
	Page<OrganizationMembership> findByOrganizationIdAndActiveTrue(UUID organizationId, Pageable pageable);

	/**
	 * Returns memberships that grant access to a specific establishment within an
	 * organization.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param pageable pagination information
	 * @return a page of matching memberships
	 */
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

	/**
	 * Returns active memberships that grant access to a specific establishment
	 * within an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param establishmentId the establishment identifier
	 * @param pageable pagination information
	 * @return a page of matching active memberships
	 */
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

	/**
	 * Finds a membership by organization and user id.
	 *
	 * @param organizationId the organization identifier
	 * @param userId the user identifier
	 * @return the matching membership, if present
	 */
	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	/**
	 * Finds a membership by id scoped to an organization.
	 *
	 * @param id the membership identifier
	 * @param organizationId the organization identifier
	 * @return the matching membership, if present
	 */
	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findByIdAndOrganizationId(UUID id, UUID organizationId);

	/**
	 * Returns the earliest active membership for a user.
	 *
	 * @param userId the user identifier
	 * @return the first active membership, if present
	 */
	@EntityGraph(attributePaths = {"organization", "user", "accessibleEstablishments"})
	Optional<OrganizationMembership> findFirstByUserIdAndActiveTrueOrderByCreatedAtAsc(UUID userId);
}
