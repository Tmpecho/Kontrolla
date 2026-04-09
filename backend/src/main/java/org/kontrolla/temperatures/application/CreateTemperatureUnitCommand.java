package org.kontrolla.temperatures.application;

import org.kontrolla.temperatures.domain.TemperatureUnitType;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CreateTemperatureUnitCommand(
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature
) {
}
