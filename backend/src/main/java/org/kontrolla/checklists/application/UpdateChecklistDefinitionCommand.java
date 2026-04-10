package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.util.List;

/**
 * Command for creating a new version of an existing checklist definition.
 *
 * @param serviceArea the checklist service area
 * @param title the checklist title
 * @param description the checklist description
 * @param status the desired status for the new version
 * @param tasks the task definitions
 * @param schedules the schedule definitions
 */
public record UpdateChecklistDefinitionCommand(
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		ChecklistDefinitionStatus status,
		List<ChecklistDefinitionTaskInput> tasks,
		List<ChecklistDefinitionScheduleInput> schedules
) {
}
