package org.kontrolla.checklists.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.kontrolla.checklists.domain.ChecklistScheduleType;
import org.kontrolla.checklists.domain.ChecklistServiceArea;
import org.kontrolla.checklists.domain.ChecklistTaskKind;

/**
 * Request payload for creating a checklist definition.
 *
 * @param title checklist title
 * @param description optional checklist description
 * @param serviceArea service area covered by the checklist
 * @param tasks task definitions included in the checklist
 * @param schedules optional schedules attached to the checklist
 */
public record CreateChecklistDefinitionRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String description,
    @NotNull ChecklistServiceArea serviceArea,
    @NotEmpty List<@Valid ChecklistTaskRequest> tasks,
    List<@Valid ChecklistScheduleRequest> schedules) {

  /**
   * Request payload describing a single task in a checklist definition.
   *
   * @param title task title
   * @param details optional task details
   * @param taskKind type of task to perform
   * @param required whether the task must be completed
   * @param sortOrder display order of the task
   * @param measurementUnit measurement unit for measurement tasks
   * @param minimumAllowedValue minimum accepted measured value
   * @param maximumAllowedValue maximum accepted measured value
   */
  public record ChecklistTaskRequest(
      @NotBlank @Size(max = 500) String title,
      @Size(max = 1000) String details,
      @NotNull ChecklistTaskKind taskKind,
      boolean required,
      @Min(0) int sortOrder,
      @Size(max = 32) String measurementUnit,
      BigDecimal minimumAllowedValue,
      BigDecimal maximumAllowedValue) {}

  /**
   * Request payload describing a checklist schedule.
   *
   * @param scheduleType schedule recurrence type
   * @param startDate first active date for the schedule
   * @param endDate optional final active date
   * @param dueTime time of day when runs become due
   * @param weekdayMask bitmask describing active weekdays
   * @param dayOfMonth day of month used by monthly schedules
   * @param timezone timezone used to evaluate the schedule
   * @param active whether the schedule should be active
   */
  public record ChecklistScheduleRequest(
      @NotNull ChecklistScheduleType scheduleType,
      @NotNull LocalDate startDate,
      LocalDate endDate,
      LocalTime dueTime,
      @Min(0) @Max(127) Integer weekdayMask,
      @Min(1) @Max(31) Integer dayOfMonth,
      @Size(max = 64) String timezone,
      Boolean active) {}
}
