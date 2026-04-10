package org.kontrolla.temperatures.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

/**
 * Request payload for creating a temperature unit.
 *
 * @param name the unit name
 * @param location the unit location within the establishment
 * @param type the type of unit
 * @param dueByTime the daily deadline for logging a reading
 * @param minimumTemperature the minimum allowed temperature in Celsius
 * @param maximumTemperature the maximum allowed temperature in Celsius
 */
public record CreateTemperatureUnitRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String location,
    @NotNull TemperatureUnitType type,
    @NotNull LocalTime dueByTime,
    @NotNull BigDecimal minimumTemperature,
    @NotNull BigDecimal maximumTemperature) {}
