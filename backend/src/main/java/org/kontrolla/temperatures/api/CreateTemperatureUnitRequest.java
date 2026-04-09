package org.kontrolla.temperatures.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.temperatures.domain.TemperatureUnitType;

import java.math.BigDecimal;
import java.time.LocalTime;

public record CreateTemperatureUnitRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String location,
    @NotNull TemperatureUnitType type,
    @NotNull LocalTime dueByTime,
    @NotNull BigDecimal minimumTemperature,
    @NotNull BigDecimal maximumTemperature
) {
}
