package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, UUID> {

	@EntityGraph(attributePaths = {
			"checklistDefinition",
			"establishment",
			"runItems",
			"runItems.response",
			"runItems.sourceChecklistItemDefinition",
			"assignments",
			"assignments.assignedUser",
			"assignments.assignedByUser",
			"events",
			"completedByUser",
			"createdByUser"
	})
	Optional<ChecklistRun> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	Page<ChecklistRun> findByEstablishmentIdAndServiceArea(UUID establishmentId, ChecklistServiceArea serviceArea, Pageable pageable);

	Page<ChecklistRun> findByEstablishmentIdAndServiceAreaAndStatusIn(
			UUID establishmentId,
			ChecklistServiceArea serviceArea,
			Collection<ChecklistRunStatus> statuses,
			Pageable pageable
	);

	long countByEstablishmentIdAndStatusInAndDueAtBefore(
			UUID establishmentId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);
}
