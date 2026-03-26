package org.kontrolla.checklists.api;

import org.kontrolla.checklists.domain.ChecklistDefinition;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistItemDefinition;
import org.kontrolla.checklists.domain.ChecklistResponseType;
import org.kontrolla.checklists.domain.ChecklistSchedule;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ChecklistDefinitionResponse(
		UUID id,
		UUID establishmentId,
		ChecklistServiceArea serviceArea,
		String title,
		String description,
		ChecklistDefinitionStatus status,
		UUID createdByUserId,
		UUID updatedByUserId,
		Instant createdAt,
		Instant updatedAt,
		List<ChecklistItemDefinitionResponse> items,
		List<ChecklistScheduleResponse> schedules
) {

	public static ChecklistDefinitionResponse from(ChecklistDefinition checklistDefinition) {
		return new ChecklistDefinitionResponse(
				checklistDefinition.getId(),
				checklistDefinition.getEstablishment().getId(),
				checklistDefinition.getServiceArea(),
				checklistDefinition.getTitle(),
				checklistDefinition.getDescription(),
				checklistDefinition.getStatus(),
				checklistDefinition.getCreatedByUser().getId(),
				checklistDefinition.getUpdatedByUser().getId(),
				checklistDefinition.getCreatedAt(),
				checklistDefinition.getUpdatedAt(),
				checklistDefinition.getItems().stream()
						.sorted(Comparator.comparingInt(ChecklistItemDefinition::getSortOrder))
						.map(ChecklistItemDefinitionResponse::from)
						.toList(),
				checklistDefinition.getSchedules().stream()
						.map(ChecklistScheduleResponse::from)
						.toList()
		);
	}

	public record ChecklistItemDefinitionResponse(
			UUID id,
			String prompt,
			String instructionText,
			ChecklistResponseType responseType,
			boolean required,
			int sortOrder
	) {

		private static ChecklistItemDefinitionResponse from(ChecklistItemDefinition item) {
			return new ChecklistItemDefinitionResponse(
					item.getId(),
					item.getPrompt(),
					item.getInstructionText(),
					item.getResponseType(),
					item.isRequired(),
					item.getSortOrder()
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
					schedule.isActive(),
					schedule.getCreatedByUser().getId(),
					schedule.getUpdatedByUser().getId()
			);
		}
	}
}
