package org.kontrolla.temperatures.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.kontrolla.temperatures.application.TemperatureLogEntryView;

/**
 * API response describing a logged temperature reading.
 *
 * @param id the log entry identifier
 * @param measuredAt when the temperature was measured
 * @param temperatureCelsius the measured temperature in Celsius
 * @param note optional note associated with the reading
 * @param loggedByName display name of the user who recorded the reading
 */
public record TemperatureLogEntryResponse(
    UUID id, Instant measuredAt, BigDecimal temperatureCelsius, String note, String loggedByName) {

  /**
   * Maps an application view to the API response shape.
   *
   * @param view the source view
   * @return the mapped response
   */
  public static TemperatureLogEntryResponse from(TemperatureLogEntryView view) {
    return new TemperatureLogEntryResponse(
        view.id(), view.measuredAt(), view.temperatureCelsius(), view.note(), view.loggedByName());
  }
}
