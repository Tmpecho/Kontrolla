package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.util.List;

public record UpdateChecklistDefinitionCommand(
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		ChecklistDefinitionStatus status,
		List<ChecklistDefinitionTaskInput> tasks,
		List<ChecklistDefinitionScheduleInput> schedules
) {
}
