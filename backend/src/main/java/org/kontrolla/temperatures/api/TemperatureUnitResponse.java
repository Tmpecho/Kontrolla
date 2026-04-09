package org.kontrolla.temperatures.api;

import org.kontrolla.temperatures.application.TemperatureUnitView;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TemperatureUnitResponse(
    UUID id,
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature,
    List<TemperatureLogEntryResponse> logs
) {

  public static TemperatureUnitResponse from(TemperatureUnitView view) {
    return new TemperatureUnitResponse(
        view.id(),
        view.name(),
        view.location(),
        view.type(),
        view.dueByTime(),
        view.minimumTemperature(),
        view.maximumTemperature(),
        view.logs().stream().map(TemperatureLogEntryResponse::from).toList()
    );
  }
}
