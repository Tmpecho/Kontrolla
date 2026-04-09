package org.kontrolla.temperatures.application;

import org.kontrolla.iam.domain.User;
import org.kontrolla.temperatures.domain.TemperatureLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TemperatureLogEntryView(
    UUID id,
    Instant measuredAt,
    BigDecimal temperatureCelsius,
    String note,
    String loggedByName
) {

  public static TemperatureLogEntryView from(TemperatureLog log) {
    return new TemperatureLogEntryView(
        log.getId(),
        log.getMeasuredAt(),
        log.getTemperatureCelsius(),
        log.getNote(),
        formatUserDisplayName(log.getLoggedByUser())
    );
  }

  private static String formatUserDisplayName(User user) {
    return (user.getFirstName() + " " + user.getLastName()).trim();
  }
}
