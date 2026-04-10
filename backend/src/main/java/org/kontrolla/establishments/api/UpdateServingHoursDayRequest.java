package org.kontrolla.establishments.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.establishments.application.UpdateServingHoursDayCommand;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request payload for updating serving hours for a single weekday.
 *
 * @param dayOfWeek the weekday to update
 * @param closed whether the establishment is closed that day
 * @param opensAt the opening time, if open
 * @param closesAt the closing time, if open
 */
public record UpdateServingHoursDayRequest(
    @NotNull DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  /**
   * Converts the request payload to the application command model.
   *
   * @return the corresponding update command
   */
  public UpdateServingHoursDayCommand toCommand() {
    return new UpdateServingHoursDayCommand(dayOfWeek, closed, opensAt, closesAt);
  }
}
