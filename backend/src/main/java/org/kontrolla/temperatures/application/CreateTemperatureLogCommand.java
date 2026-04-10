package org.kontrolla.temperatures.application;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Command for recording a measured temperature for a unit.
 *
 * @param temperatureCelsius the measured temperature in Celsius
 * @param measuredAt when the temperature was measured
 * @param note optional note explaining the reading
 */
public record CreateTemperatureLogCommand(
    BigDecimal temperatureCelsius, Instant measuredAt, String note) {}
