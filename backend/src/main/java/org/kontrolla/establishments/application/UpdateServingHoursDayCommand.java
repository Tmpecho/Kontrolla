package org.kontrolla.establishments.application;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Command for updating serving hours for a single weekday.
 *
 * @param dayOfWeek the weekday to update
 * @param closed whether the establishment is closed that day
 * @param opensAt the opening time, if open
 * @param closesAt the closing time, if open
 */
public record UpdateServingHoursDayCommand(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {
}
