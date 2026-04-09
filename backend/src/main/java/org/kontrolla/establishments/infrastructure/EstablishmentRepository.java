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

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {

	@EntityGraph(attributePaths = {"organization"})
	Page<Establishment> findByOrganizationId(UUID organizationId, Pageable pageable);

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

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findByIdAndOrganizationId(UUID id, UUID organizationId);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdAndStatusOrderByCreatedAtAsc(
			UUID organizationId,
			EstablishmentStatus status
	);

	@EntityGraph(attributePaths = {"organization"})
	Optional<Establishment> findFirstByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

	@EntityGraph(attributePaths = {"organization"})
	List<Establishment> findByOrganizationIdAndIdIn(UUID organizationId, Collection<UUID> ids);

	@EntityGraph(attributePaths = {"organization"})
	List<Establishment> findByStatus(EstablishmentStatus status);
}
