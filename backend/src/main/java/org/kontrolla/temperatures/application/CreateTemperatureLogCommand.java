package org.kontrolla.temperatures.application;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTemperatureLogCommand(
    BigDecimal temperatureCelsius,
    Instant measuredAt,
    String note
) {
}
