package org.kontrolla.temperatures.application;

import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TemperatureUnitView(
    UUID id,
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature,
    List<TemperatureLogEntryView> logs
) {

  private static final int RECENT_LOG_LIMIT = 7;

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
            .toList()
    );
  }
}
