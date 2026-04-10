package org.kontrolla.temperatures.api;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request payload for creating a temperature log entry.
 *
 * @param temperatureCelsius the measured temperature in Celsius
 * @param measuredAt when the temperature was measured
 * @param note optional note associated with the reading
 */
public record CreateTemperatureLogRequest(
    @NotNull BigDecimal temperatureCelsius, @NotNull Instant measuredAt, String note) {}
