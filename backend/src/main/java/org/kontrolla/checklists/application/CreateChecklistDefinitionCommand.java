package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.util.List;

/**
 * Command for creating a checklist definition.
 *
 * @param serviceArea the checklist service area
 * @param title the checklist title
 * @param description the checklist description
 * @param tasks the task definitions
 * @param schedules the schedule definitions
 */
public record CreateChecklistDefinitionCommand(
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		List<ChecklistDefinitionTaskInput> tasks,
		List<ChecklistDefinitionScheduleInput> schedules
) {
}
