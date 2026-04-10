package org.kontrolla.establishments.application;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateServingHoursDayCommand(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {
}
