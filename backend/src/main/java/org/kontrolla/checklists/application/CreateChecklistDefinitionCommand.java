package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.util.List;

public record CreateChecklistDefinitionCommand(
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		List<ChecklistDefinitionTaskInput> tasks,
		List<ChecklistDefinitionScheduleInput> schedules
) {
}
