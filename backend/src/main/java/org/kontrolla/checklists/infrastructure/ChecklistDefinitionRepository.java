package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChecklistDefinitionRepository extends JpaRepository<ChecklistDefinition, UUID> {

	@EntityGraph(attributePaths = {"establishment", "createdByUser", "updatedByUser", "items", "schedules"})
	Optional<ChecklistDefinition> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	@EntityGraph(attributePaths = {"items", "schedules"})
	Page<ChecklistDefinition> findByEstablishmentIdAndServiceArea(UUID establishmentId, ChecklistServiceArea serviceArea, Pageable pageable);
}
