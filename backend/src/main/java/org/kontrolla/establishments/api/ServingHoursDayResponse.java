package org.kontrolla.establishments.api;

import org.kontrolla.establishments.application.ServingHoursDayView;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ServingHoursDayResponse(
    DayOfWeek dayOfWeek,
    boolean closed,
    LocalTime opensAt,
    LocalTime closesAt
) {

  public static ServingHoursDayResponse from(ServingHoursDayView view) {
    return new ServingHoursDayResponse(
        view.dayOfWeek(),
        view.closed(),
        view.opensAt(),
        view.closesAt()
    );
  }
}
