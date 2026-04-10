package org.kontrolla.checklists.application;

import java.time.LocalDate;
import java.time.LocalTime;
import org.kontrolla.checklists.domain.ChecklistScheduleType;

/**
 * Application input describing one checklist schedule.
 *
 * @param scheduleType the schedule type
 * @param startDate the schedule start date
 * @param endDate the optional schedule end date
 * @param dueTime the due time for generated runs
 * @param weekdayMask bitmask for weekly schedules
 * @param dayOfMonth day of month for monthly schedules
 * @param timezone the schedule timezone
 * @param active whether the schedule is active
 */
public record ChecklistDefinitionScheduleInput(
    ChecklistScheduleType scheduleType,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime dueTime,
    Integer weekdayMask,
    Integer dayOfMonth,
    String timezone,
    Boolean active) {}
