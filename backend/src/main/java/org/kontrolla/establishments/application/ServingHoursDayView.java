package org.kontrolla.establishments.application;

import org.kontrolla.establishments.domain.EstablishmentServingHours;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Application-layer view of serving hours for a single day.
 *
 * @param dayOfWeek the weekday represented by the entry
 * @param closed whether the establishment is closed on that day
 * @param opensAt the opening time, if open
 * @param closesAt the closing time, if open
 */
public record ServingHoursDayView(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  /**
   * Maps serving-hours entity data to the application view model.
   *
   * @param hours the serving-hours entity
   * @return the mapped view
   */
  public static ServingHoursDayView from(EstablishmentServingHours hours) {
    return new ServingHoursDayView(
        hours.getDayOfWeek(),
        hours.isClosed(),
        hours.getOpensAt(),
        hours.getClosesAt()
    );
  }

  /**
   * Creates a closed-day view for a weekday without persisted hours.
   *
   * @param dayOfWeek the weekday to represent
   * @return a closed-day view
   */
  public static ServingHoursDayView closed(DayOfWeek dayOfWeek) {
    return new ServingHoursDayView(dayOfWeek, true, null, null);
  }
}
