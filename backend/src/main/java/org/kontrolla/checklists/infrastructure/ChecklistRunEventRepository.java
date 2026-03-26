package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistRunEventRepository extends JpaRepository<ChecklistRunEvent, UUID> {

	List<ChecklistRunEvent> findByChecklistRunIdOrderByOccurredAtAsc(UUID checklistRunId);
}
