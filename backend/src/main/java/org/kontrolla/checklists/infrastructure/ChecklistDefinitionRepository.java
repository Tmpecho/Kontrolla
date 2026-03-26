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

public interface ChecklistDefinitionRepository extends JpaRepository<ChecklistDefinition, UUID> {

	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	Optional<ChecklistDefinition> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	Page<ChecklistDefinition> findByEstablishmentIdAndServiceAreaAndStatus(
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			ChecklistDefinitionStatus status,
			Pageable pageable
	);

	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	List<ChecklistDefinition> findByDefinitionGroupIdAndEstablishmentIdOrderByVersionNumberAsc(UUID definitionGroupId, UUID establishmentId);

	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "tasks", "schedules"})
	List<ChecklistDefinition> findByEstablishmentIdAndStatus(UUID establishmentId, ChecklistDefinitionStatus status);
}
