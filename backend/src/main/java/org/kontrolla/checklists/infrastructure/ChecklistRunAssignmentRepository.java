package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for checklist run assignments.
 */
public interface ChecklistRunAssignmentRepository extends JpaRepository<ChecklistRunAssignment, UUID> {

	/**
	 * Lists all assignments for a checklist run.
	 *
	 * @param checklistRunId checklist run identifier
	 * @return assignments attached to the run
	 */
	List<ChecklistRunAssignment> findByChecklistRunId(UUID checklistRunId);

	/**
	 * Finds a specific assignment within a checklist run.
	 *
	 * @param id assignment identifier
	 * @param checklistRunId checklist run identifier
	 * @return the matching assignment, if present
	 */
	Optional<ChecklistRunAssignment> findByIdAndChecklistRunId(UUID id, UUID checklistRunId);

	/**
	 * Deletes all assignments for a checklist run.
	 *
	 * @param checklistRunId checklist run identifier
	 */
	void deleteByChecklistRunId(UUID checklistRunId);
}
