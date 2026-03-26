package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRun;
import org.kontrolla.checklists.domain.ChecklistRunStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
	@Query("""
			select distinct run
			from ChecklistRun run
			left join run.assignments assignment
			where run.establishment.id = :establishmentId
			  and run.serviceArea = :serviceArea
			  and (:statusesEmpty = true or run.status in :statuses)
			  and (:assignedUserId is null or assignment.assignedUser.id = :assignedUserId)
			  and (:dueFrom is null or run.dueAt >= :dueFrom)
			  and (:dueTo is null or run.dueAt <= :dueTo)
			""")
	Page<ChecklistRun> search(
			@Param("establishmentId") UUID establishmentId,
			@Param("serviceArea") ChecklistServiceArea serviceArea,
			@Param("statuses") Collection<ChecklistRunStatus> statuses,
			@Param("statusesEmpty") boolean statusesEmpty,
			@Param("assignedUserId") UUID assignedUserId,
			@Param("dueFrom") Instant dueFrom,
			@Param("dueTo") Instant dueTo,
			Pageable pageable
	);

	long countByEstablishmentIdAndStatusInAndDueAtBefore(
			UUID establishmentId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);

	List<ChecklistRun> findByEstablishmentIdAndStatusInAndDueAtBefore(
			UUID establishmentId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);

	boolean existsByChecklistDefinitionIdAndDueAt(UUID checklistDefinitionId, Instant dueAt);
}
