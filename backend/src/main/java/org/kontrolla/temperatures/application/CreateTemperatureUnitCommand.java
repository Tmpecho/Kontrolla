package org.kontrolla.temperatures.application;

import java.math.BigDecimal;
import java.time.LocalTime;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

/**
 * Command for creating a temperature-controlled unit for an establishment.
 *
 * @param name the unit name
 * @param location the unit location within the establishment
 * @param type the type of temperature unit
 * @param dueByTime the daily deadline for logging a reading
 * @param minimumTemperature the minimum allowed temperature in Celsius
 * @param maximumTemperature the maximum allowed temperature in Celsius
 */
public record CreateTemperatureUnitCommand(
    String name,
    String location,
    TemperatureUnitType type,
    LocalTime dueByTime,
    BigDecimal minimumTemperature,
    BigDecimal maximumTemperature) {}
