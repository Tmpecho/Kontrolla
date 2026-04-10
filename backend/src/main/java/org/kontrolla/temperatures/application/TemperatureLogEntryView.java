package org.kontrolla.temperatures.application;

import org.kontrolla.iam.domain.User;
import org.kontrolla.temperatures.domain.TemperatureLog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Application-layer view of a logged temperature reading.
 *
 * @param id the log entry identifier
 * @param measuredAt when the temperature was measured
 * @param temperatureCelsius the measured temperature in Celsius
 * @param note optional note associated with the reading
 * @param loggedByName display name of the user who recorded the reading
 */
public record TemperatureLogEntryView(
    UUID id,
    Instant measuredAt,
    BigDecimal temperatureCelsius,
    String note,
    String loggedByName
) {

  /**
   * Maps a temperature log entity to the application view model.
   *
   * @param log the domain log to map
   * @return the mapped view
   */
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
    String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
    return fullName.isEmpty() ? user.getEmail() : fullName;
  }
}
