package org.kontrolla.temperatures.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTemperatureLogRequest(
    @NotNull BigDecimal temperatureCelsius,
    @NotNull Instant measuredAt,
    String note
) {
}
