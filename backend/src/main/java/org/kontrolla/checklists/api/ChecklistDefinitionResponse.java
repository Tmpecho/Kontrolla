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
