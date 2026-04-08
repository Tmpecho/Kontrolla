package org.kontrolla.checklists.application;

import org.kontrolla.checklists.domain.ChecklistScheduleType;

import java.time.LocalDate;
import java.time.LocalTime;

public record ChecklistDefinitionScheduleInput(
		ChecklistScheduleType scheduleType,
		LocalDate startDate,
		LocalDate endDate,
		LocalTime dueTime,
		Integer weekdayMask,
		Integer dayOfMonth,
		String timezone,
		Boolean active
) {
}
