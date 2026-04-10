package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for loading and querying checklist definitions.
 */
public interface ChecklistDefinitionRepository extends JpaRepository<ChecklistDefinition, UUID> {

	/**
	 * Finds a checklist definition within an establishment by identifier.
	 *
	 * @param id checklist definition identifier
	 * @param establishmentId establishment identifier
	 * @return the matching checklist definition, if present
	 */
	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	Optional<ChecklistDefinition> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	/**
	 * Lists checklist definitions for an establishment, service area, and status.
	 *
	 * @param establishmentId establishment identifier
	 * @param serviceArea service area to filter by
	 * @param status definition status to filter by
	 * @param pageable paging configuration
	 * @return a page of matching checklist definitions
	 */
	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	Page<ChecklistDefinition> findByEstablishmentIdAndServiceAreaAndStatus(
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			ChecklistDefinitionStatus status,
			Pageable pageable
	);

	/**
	 * Lists all versions of a checklist definition group for an establishment.
	 *
	 * @param definitionGroupId definition group identifier
	 * @param establishmentId establishment identifier
	 * @return matching definition versions ordered by version number
	 */
	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	List<ChecklistDefinition> findByDefinitionGroupIdAndEstablishmentIdOrderByVersionNumberAsc(UUID definitionGroupId, UUID establishmentId);

	/**
	 * Lists checklist definitions in an establishment with the requested status.
	 *
	 * @param establishmentId establishment identifier
	 * @param status definition status to filter by
	 * @return matching checklist definitions
	 */
	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	List<ChecklistDefinition> findByEstablishmentIdAndStatus(UUID establishmentId, ChecklistDefinitionStatus status);
}
