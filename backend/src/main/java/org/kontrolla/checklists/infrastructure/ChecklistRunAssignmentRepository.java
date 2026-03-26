package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRunAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistRunAssignmentRepository extends JpaRepository<ChecklistRunAssignment, UUID> {

	List<ChecklistRunAssignment> findByChecklistRunId(UUID checklistRunId);

	void deleteByChecklistRunId(UUID checklistRunId);
}
