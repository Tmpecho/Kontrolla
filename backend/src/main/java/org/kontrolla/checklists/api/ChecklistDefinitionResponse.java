package org.kontrolla.checklists.api;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskDefinition;
import org.kontrolla.checklists.domain.ChecklistTaskKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Response payload describing a checklist definition and its configured tasks and schedules.
 *
 * @param id identifier of the checklist definition
 * @param definitionGroupId identifier shared across versioned definitions
 * @param versionNumber version number of this definition
 * @param establishmentId establishment that owns the definition
 * @param serviceArea service area covered by the checklist
 * @param title checklist title
 * @param description checklist description
 * @param status current definition status
 * @param effectiveFrom instant when this definition version became effective
 * @param effectiveTo instant when this definition version stopped being effective
 * @param createdByUserId user who created the definition
 * @param updatedByUserId user who last updated the definition
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 * @param tasks task definitions included in the checklist
 * @param schedules schedules attached to the checklist
 */
public record ChecklistDefinitionResponse(
		UUID id,
		UUID definitionGroupId,
		int versionNumber,
		UUID establishmentId,
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		ChecklistDefinitionStatus status,
		Instant effectiveFrom,
		Instant effectiveTo,
		UUID createdByUserId,
		UUID updatedByUserId,
		Instant createdAt,
		Instant updatedAt,
		List<ChecklistTaskDefinitionResponse> tasks,
		List<ChecklistScheduleResponse> schedules
) {

	/**
	 * Maps a checklist definition entity to an API response.
	 *
	 * @param checklistDefinition checklist definition entity to convert
	 * @return the mapped response
	 */
	public static ChecklistDefinitionResponse from(ChecklistDefinition checklistDefinition) {
		return new ChecklistDefinitionResponse(
				checklistDefinition.getId(),
				checklistDefinition.getDefinitionGroupId(),
				checklistDefinition.getVersionNumber(),
				checklistDefinition.getEstablishment().getId(),
				checklistDefinition.getServiceArea(),
				checklistDefinition.getTitle(),
				checklistDefinition.getDescription(),
				checklistDefinition.getStatus(),
				checklistDefinition.getEffectiveFrom(),
				checklistDefinition.getEffectiveTo(),
				checklistDefinition.getCreatedByUser().getId(),
				checklistDefinition.getUpdatedByUser().getId(),
				checklistDefinition.getCreatedAt(),
				checklistDefinition.getUpdatedAt(),
				checklistDefinition.getTasks().stream()
						.sorted(Comparator.comparingInt(ChecklistTaskDefinition::getSortOrder))
						.map(ChecklistTaskDefinitionResponse::from)
						.toList(),
				checklistDefinition.getSchedules().stream()
						.map(ChecklistScheduleResponse::from)
						.toList()
		);
	}

	/**
	 * Response payload describing a single task definition within a checklist definition.
	 *
	 * @param id identifier of the task definition
	 * @param title task title
	 * @param details optional task details
	 * @param taskKind type of task to perform
	 * @param required whether the task is required
	 * @param sortOrder display order of the task
	 * @param measurementUnit measurement unit for measurement tasks
	 * @param minimumAllowedValue lower accepted measured value
	 * @param maximumAllowedValue upper accepted measured value
	 */
	public record ChecklistTaskDefinitionResponse(
			UUID id,
			String title,
			String details,
			ChecklistTaskKind taskKind,
			boolean required,
			int sortOrder,
			String measurementUnit,
			BigDecimal minimumAllowedValue,
			BigDecimal maximumAllowedValue
	) {

		private static ChecklistTaskDefinitionResponse from(ChecklistTaskDefinition task) {
			return new ChecklistTaskDefinitionResponse(
					task.getId(),
					task.getTitle(),
					task.getDetails(),
					task.getTaskKind(),
					task.isRequired(),
					task.getSortOrder(),
					task.getMeasurementUnit(),
					task.getMinimumAllowedValue(),
					task.getMaximumAllowedValue()
			);
		}
	}

	/**
	 * Response payload describing a checklist schedule.
	 *
	 * @param id identifier of the schedule
	 * @param scheduleType schedule recurrence type
	 * @param startDate first active date for the schedule
	 * @param endDate optional final active date
	 * @param dueTime time of day when checklist runs become due
	 * @param weekdayMask bitmask describing active weekdays
	 * @param dayOfMonth day of month used by monthly schedules
	 * @param timezone timezone used to evaluate the schedule
	 * @param active whether the schedule is active
	 * @param createdByUserId user who created the schedule
	 * @param updatedByUserId user who last updated the schedule
	 */
	public record ChecklistScheduleResponse(
			UUID id,
			ChecklistScheduleType scheduleType,
			LocalDate startDate,
			LocalDate endDate,
			LocalTime dueTime,
			Integer weekdayMask,
			Integer dayOfMonth,
			String timezone,
			boolean active,
			UUID createdByUserId,
			UUID updatedByUserId
	) {

		private static ChecklistScheduleResponse from(ChecklistSchedule schedule) {
			return new ChecklistScheduleResponse(
					schedule.getId(),
					schedule.getScheduleType(),
					schedule.getStartDate(),
					schedule.getEndDate(),
					schedule.getDueTime(),
					schedule.getWeekdayMask(),
					schedule.getDayOfMonth(),
					schedule.getTimezone(),
					schedule.isActive(),
					schedule.getCreatedByUser().getId(),
					schedule.getUpdatedByUser().getId()
			);
		}
	}
}
