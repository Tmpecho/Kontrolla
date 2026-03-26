package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.checklists.domain.ChecklistDefinitionStatus;
import org.kontrolla.checklists.domain.ChecklistTaskKind;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record UpdateChecklistDefinitionRequest(
		@NotBlank @Size(max = 255) String title,
		@Size(max = 2000) String description,
		@NotNull ChecklistServiceArea serviceArea,
		ChecklistDefinitionStatus status,
		@NotEmpty List<@Valid ChecklistTaskRequest> tasks,
		List<@Valid ChecklistScheduleRequest> schedules
) {

	public record ChecklistTaskRequest(
			@NotBlank @Size(max = 500) String title,
			@Size(max = 1000) String details,
			@NotNull ChecklistTaskKind taskKind,
			boolean required,
			@Min(0) int sortOrder,
			@Size(max = 32) String measurementUnit,
			BigDecimal minimumAllowedValue,
			BigDecimal maximumAllowedValue
	) {
	}

	public record ChecklistScheduleRequest(
			@NotNull ChecklistScheduleType scheduleType,
			@NotNull LocalDate startDate,
			LocalDate endDate,
			LocalTime dueTime,
			@Min(0) @Max(127) Integer weekdayMask,
			@Min(1) @Max(31) Integer dayOfMonth,
			@Size(max = 64) String timezone,
			Boolean active
	) {
	}
}
