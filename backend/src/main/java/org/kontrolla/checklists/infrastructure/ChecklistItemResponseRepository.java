package org.kontrolla.checklists.infrastructure;

import org.kontrolla.checklists.domain.ChecklistItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistItemResponseRepository extends JpaRepository<ChecklistItemResponse, UUID> {

	List<ChecklistItemResponse> findByChecklistRunItemChecklistRunId(UUID checklistRunId);
}
