package org.kontrolla.temperatures.api;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.kontrolla.temperatures.application.TemperatureUnitView;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

/**
 * API response describing a temperature unit and its recent logs.
 *
 * @param id the unit identifier
 * @param name the unit name
 * @param location the unit location
 * @param type the unit type
 * @param dueByTime the daily logging deadline
 * @param minimumTemperature the minimum allowed temperature
 * @param maximumTemperature the maximum allowed temperature
 * @param logs the recent temperature logs
 */
public record TemperatureUnitResponse(
    UUID id,
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature,
    List<TemperatureLogEntryResponse> logs) {

  /**
   * Maps an application view to the API response shape.
   *
   * @param view the source view
   * @return the mapped response
   */
  public static TemperatureUnitResponse from(TemperatureUnitView view) {
    return new TemperatureUnitResponse(
        view.id(),
        view.name(),
        view.location(),
        view.type(),
        view.dueByTime(),
        view.minimumTemperature(),
        view.maximumTemperature(),
        view.logs().stream().map(TemperatureLogEntryResponse::from).toList());
  }
}
