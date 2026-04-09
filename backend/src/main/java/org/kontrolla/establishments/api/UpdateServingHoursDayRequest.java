package org.kontrolla.establishments.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.establishments.application.UpdateServingHoursDayCommand;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateServingHoursDayRequest(
    @NotNull DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  public UpdateServingHoursDayCommand toCommand() {
    return new UpdateServingHoursDayCommand(dayOfWeek, closed, opensAt, closesAt);
  }
}
