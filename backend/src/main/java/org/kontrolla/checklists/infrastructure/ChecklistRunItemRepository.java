package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistRunItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistRunItemRepository extends JpaRepository<ChecklistRunItem, UUID> {

	List<ChecklistRunItem> findByChecklistRunIdOrderBySortOrderAsc(UUID checklistRunId);
}
