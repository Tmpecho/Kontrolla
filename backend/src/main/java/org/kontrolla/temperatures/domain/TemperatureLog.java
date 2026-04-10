package org.kontrolla.temperatures.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.iam.domain.User;

/** Persisted temperature reading recorded for a temperature-controlled unit. */
@Getter
@Entity
@Table(name = "temperature_logs")
public class TemperatureLog extends AbstractAuditableUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "temperature_unit_id", nullable = false)
  private TemperatureUnit temperatureUnit;

  @Column(name = "measured_at", nullable = false)
  private Instant measuredAt;

  @Column(name = "temperature_celsius", nullable = false, precision = 6, scale = 2)
  private BigDecimal temperatureCelsius;

  @Column(length = 1000)
  private String note;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "logged_by_user_id", nullable = false)
  private User loggedByUser;

  protected TemperatureLog() {}

  /**
   * Creates a temperature log entry.
   *
   * @param measuredAt when the temperature was measured
   * @param temperatureCelsius the measured temperature in Celsius
   * @param note optional note associated with the reading
   * @param loggedByUser the user who recorded the reading
   */
  public TemperatureLog(
      Instant measuredAt, BigDecimal temperatureCelsius, String note, User loggedByUser) {
    this.measuredAt = measuredAt;
    this.temperatureCelsius = temperatureCelsius;
    this.note = note;
    this.loggedByUser = loggedByUser;
  }

  void attachTo(TemperatureUnit temperatureUnit) {
    this.temperatureUnit = temperatureUnit;
  }
}
