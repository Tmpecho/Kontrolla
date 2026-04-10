package org.kontrolla.establishments.infrastructure;

import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.establishments.domain.EstablishmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for establishment persistence and organization-scoped queries.
 */
public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {

	/**
	 * Returns establishments for an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param pageable pagination information
	 * @return a page of establishments
	 */
	@EntityGraph(attributePaths = {"organization"})
	Page<Establishment> findByOrganizationId(UUID organizationId, Pageable pageable);

	/**
	 * Returns establishments within an organization that are accessible to a
	 * specific user through membership rules.
	 *
	 * @param organizationId the organization identifier
	 * @param userId the user identifier
	 * @param pageable pagination information
	 * @return a page of accessible establishments
	 */
	@EntityGraph(attributePaths = {"organization"})
	@Query(
			value = """
					select distinct establishment
					from Establishment establishment
					join OrganizationMembership membership on membership.organization.id = establishment.organization.id
					left join membership.accessibleEstablishments accessibleEstablishment
					where establishment.organization.id = :organizationId
					and membership.user.id = :userId
					and membership.active = true
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = establishment.id)
					""",
			countQuery = """
					select count(distinct establishment.id)
					from Establishment establishment
					join OrganizationMembership membership on membership.organization.id = establishment.organization.id
					left join membership.accessibleEstablishments accessibleEstablishment
					where establishment.organization.id = :organizationId
					and membership.user.id = :userId
					and membership.active = true
					and (membership.accessAllEstablishments = true or accessibleEstablishment.id = establishment.id)
					"""
	)
	Page<Establishment> findAccessibleByOrganizationIdAndUserId(
			@Param("organizationId") UUID organizationId,
			@Param("userId") UUID userId,
			Pageable pageable
	);

	/**
	 * Finds an establishment by id within an organization.
	 *
	 * @param id the establishment identifier
	 * @param organizationId the organization identifier
	 * @return the matching establishment, if present
	 */
	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findByIdAndOrganizationId(UUID id, UUID organizationId);

	/**
	 * Returns the earliest-created establishment for an organization.
	 *
	 * @param organizationId the organization identifier
	 * @return the first establishment by creation time, if present
	 */
	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

	/**
	 * Returns the earliest-created establishment with a specific status in an
	 * organization.
	 *
	 * @param organizationId the organization identifier
	 * @param status the desired establishment status
	 * @return the first matching establishment, if present
	 */
	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdAndStatusOrderByCreatedAtAsc(
			UUID organizationId,
			EstablishmentStatus status
	);

	/**
	 * Finds an establishment by case-insensitive name within an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param name the establishment name
	 * @return the matching establishment, if present
	 */
	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

	/**
	 * Returns establishments by id within an organization.
	 *
	 * @param organizationId the organization identifier
	 * @param ids the establishment identifiers
	 * @return the matching establishments
	 */
	@EntityGraph(attributePaths = {"organization"})
	List<Establishment> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

	/**
	 * Returns all establishments with the given status.
	 *
	 * @param status the desired establishment status
	 * @return the matching establishments
	 */
	@EntityGraph(attributePaths = {"organization"})
	List<Establishment> findByStatus(EstablishmentStatus status);
}
