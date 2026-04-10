package org.kontrolla.establishments.api;

import org.kontrolla.establishments.application.ServingHoursDayView;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * API response describing serving hours for a single weekday.
 *
 * @param dayOfWeek the weekday represented by the entry
 * @param closed whether the establishment is closed on that day
 * @param opensAt the opening time, if open
 * @param closesAt the closing time, if open
 */
public record ServingHoursDayResponse(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  /**
   * Maps an application view to the API response shape.
   *
   * @param view the source view
   * @return the mapped response
   */
  public static ServingHoursDayResponse from(ServingHoursDayView view) {
    return new ServingHoursDayResponse(
        view.dayOfWeek(),
        view.closed(),
        view.opensAt(),
        view.closesAt()
    );
  }
}
