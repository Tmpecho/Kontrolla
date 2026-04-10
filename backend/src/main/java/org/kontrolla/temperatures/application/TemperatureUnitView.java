package org.kontrolla.temperatures.application;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

/**
 * Application-layer view of a temperature unit and its most recent readings.
 *
 * @param id the unit identifier
 * @param name the unit name
 * @param location the unit location
 * @param type the unit type
 * @param dueByTime the daily logging deadline
 * @param minimumTemperature the minimum allowed temperature
 * @param maximumTemperature the maximum allowed temperature
 * @param logs the recent log entries for the unit
 */
public record TemperatureUnitView(
    UUID id,
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature,
    List<TemperatureLogEntryView> logs) {

  private static final int RECENT_LOG_LIMIT = 7;

  /**
   * Maps a temperature unit entity to the application view model.
   *
   * @param unit the domain entity to map
   * @return the mapped view
   */
  public static TemperatureUnitView from(TemperatureUnit unit) {
    return new TemperatureUnitView(
        unit.getId(),
        unit.getName(),
        unit.getLocation(),
        unit.getType(),
        unit.getDueByTime(),
        unit.getMinimumTemperature(),
        unit.getMaximumTemperature(),
        unit.getLogs().stream()
            .limit(RECENT_LOG_LIMIT)
            .map(TemperatureLogEntryView::from)
            .toList());
  }
}
