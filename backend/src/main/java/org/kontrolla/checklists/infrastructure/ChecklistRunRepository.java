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

/**
 * Repository for loading and querying checklist runs.
 */
public interface ChecklistRunRepository extends JpaRepository<ChecklistRun, UUID> {

	/**
	 * Finds a checklist run within an establishment by identifier.
	 *
	 * @param id checklist run identifier
	 * @param establishmentId establishment identifier
	 * @return the matching checklist run, if present
	 */
	@EntityGraph(attributePaths = {
			"checklistDefinition",
			"establishment",
			"taskExecutions",
			"taskExecutions.sourceChecklistTaskDefinition",
			"taskExecutions.resolvedByUser",
			"assignments",
			"assignments.assignedUser",
			"assignments.assignedByUser",
			"events",
			"completedByUser",
			"createdByUser"
	})
	Optional<ChecklistRun> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

	/**
	 * Searches checklist runs using service-area, status, assignment, and due-date filters.
	 *
	 * @param establishmentId establishment identifier
	 * @param serviceArea service area to filter by
	 * @param statuses run statuses to include
	 * @param assignedUserId optional assigned user filter
	 * @param dueFrom optional lower due timestamp bound
	 * @param dueTo optional upper due timestamp bound
	 * @param pageable paging configuration
	 * @return a page of matching checklist runs
	 */
	@EntityGraph(attributePaths = {
			"checklistDefinition",
			"establishment",
			"taskExecutions",
			"taskExecutions.sourceChecklistTaskDefinition",
			"taskExecutions.resolvedByUser",
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
			  and run.status in :statuses
			  and (:assignedUserId is null or assignment.assignedUser.id = :assignedUserId)
			  and (:dueFrom is null or run.dueAt >= :dueFrom)
			  and (:dueTo is null or run.dueAt <= :dueTo)
			""")
	Page<ChecklistRun> search(
			@Param("establishmentId") UUID establishmentId,
			@Param("serviceArea") ChecklistServiceArea serviceArea,
			@Param("statuses") Collection<ChecklistRunStatus> statuses,
			@Param("assignedUserId") UUID assignedUserId,
			@Param("dueFrom") Instant dueFrom,
			@Param("dueTo") Instant dueTo,
			Pageable pageable
	);

	/**
	 * Counts checklist runs that are overdue relative to the supplied timestamp.
	 *
	 * @param establishmentId establishment identifier
	 * @param statuses run statuses to include
	 * @param dueAt timestamp used as the overdue cutoff
	 * @return number of matching checklist runs
	 */
	long countByEstablishmentIdAndStatusInAndDueAtBefore(
			UUID establishmentId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);

	/**
	 * Lists checklist runs that are overdue relative to the supplied timestamp.
	 *
	 * @param establishmentId establishment identifier
	 * @param statuses run statuses to include
	 * @param dueAt timestamp used as the overdue cutoff
	 * @return matching overdue checklist runs
	 */
	List<ChecklistRun> findByEstablishmentIdAndStatusInAndDueAtBefore(
			UUID establishmentId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);

	/**
	 * Lists future checklist runs for a definition group with the requested status.
	 *
	 * @param establishmentId establishment identifier
	 * @param definitionGroupId definition group identifier
	 * @param status run status to filter by
	 * @param dueAt minimum due timestamp
	 * @return matching checklist runs ordered by due date
	 */
	List<ChecklistRun> findByEstablishmentIdAndDefinitionGroupIdAndStatusAndDueAtGreaterThanEqualOrderByDueAtAsc(
			UUID establishmentId,
			UUID definitionGroupId,
			ChecklistRunStatus status,
			Instant dueAt
	);

	/**
	 * Lists future checklist runs for a definition group with any of the requested statuses.
	 *
	 * @param establishmentId establishment identifier
	 * @param definitionGroupId definition group identifier
	 * @param statuses run statuses to include
	 * @param dueAt minimum due timestamp
	 * @return matching checklist runs ordered by due date
	 */
	List<ChecklistRun> findByEstablishmentIdAndDefinitionGroupIdAndStatusInAndDueAtGreaterThanEqualOrderByDueAtAsc(
			UUID establishmentId,
			UUID definitionGroupId,
			Collection<ChecklistRunStatus> statuses,
			Instant dueAt
	);

	/**
	 * Checks whether a checklist run already exists for a definition at a due timestamp.
	 *
	 * @param checklistDefinitionId checklist definition identifier
	 * @param dueAt due timestamp to check
	 * @return {@code true} when a matching run exists
	 */
	boolean existsByChecklistDefinitionIdAndDueAt(UUID checklistDefinitionId, Instant dueAt);

	/**
	 * Lists all checklist runs for an establishment ordered by due date.
	 *
	 * @param establishmentId establishment identifier
	 * @return checklist runs ordered by due date
	 */
	@EntityGraph(attributePaths = {
			"checklistDefinition",
			"establishment",
			"taskExecutions",
			"taskExecutions.sourceChecklistTaskDefinition",
			"taskExecutions.resolvedByUser",
			"assignments",
			"assignments.assignedUser",
			"assignments.assignedByUser",
			"events",
			"completedByUser",
			"createdByUser"
	})
	List<ChecklistRun> findByEstablishmentIdOrderByDueAtAsc(UUID establishmentId);
}
