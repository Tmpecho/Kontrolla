package org.kontrolla.establishments.application;

import org.kontrolla.establishments.domain.EstablishmentServingHours;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ServingHoursDayView(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  public static ServingHoursDayView from(EstablishmentServingHours hours) {
    return new ServingHoursDayView(
        hours.getDayOfWeek(),
        hours.isClosed(),
        hours.getOpensAt(),
        hours.getClosesAt()
    );
  }

  public static ServingHoursDayView closed(DayOfWeek dayOfWeek) {
    return new ServingHoursDayView(dayOfWeek, true, null, null);
  }
}
