package org.kontrolla.checklists.infrastructure;

import java.util.List;
import java.util.UUID;
import org.kontrolla.checklists.domain.ChecklistRunEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for checklist run events. */
public interface ChecklistRunEventRepository extends JpaRepository<ChecklistRunEvent, UUID> {

  /**
   * Lists all events for a checklist run in occurrence order.
   *
   * @param checklistRunId checklist run identifier
   * @return ordered events for the run
   */
  List<ChecklistRunEvent> findByChecklistRunIdOrderByOccurredAtAsc(UUID checklistRunId);
}
