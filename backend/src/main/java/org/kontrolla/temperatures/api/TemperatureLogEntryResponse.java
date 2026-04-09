package org.kontrolla.temperatures.api;

import org.kontrolla.temperatures.application.TemperatureLogEntryView;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TemperatureLogEntryResponse(
    UUID id,
    Instant measuredAt,
    BigDecimal temperatureCelsius,
    String note,
    String loggedByName
) {

  public static TemperatureLogEntryResponse from(TemperatureLogEntryView view) {
    return new TemperatureLogEntryResponse(
        view.id(),
        view.measuredAt(),
        view.temperatureCelsius(),
        view.note(),
        view.loggedByName()
    );
  }
}
