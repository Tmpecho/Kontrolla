package org.kontrolla.temperatures.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;
import org.kontrolla.establishments.domain.Establishment;
import org.kontrolla.organizations.domain.Organization;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persisted temperature-controlled unit that belongs to an establishment and
 * stores allowed temperature bounds.
 */
@Getter
@Entity
@Table(name = "temperature_units")
public class TemperatureUnit extends AbstractAuditableUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "establishment_id", nullable = false)
  private Establishment establishment;

  @Setter
  @Column(nullable = false, length = 255)
  private String name;

  @Setter
  @Column(nullable = false, length = 255)
  private String location;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TemperatureUnitType type;

  // Daily recurring logging deadline, for example 08:30 every day.
  @Setter
  @Column(name = "due_by_time", nullable = false)
  private LocalTime dueByTime;

  @Setter
  @Column(name = "minimum_temperature", nullable = false, precision = 6, scale = 2)
  private BigDecimal minimumTemperature;

  @Setter
  @Column(name = "maximum_temperature", nullable = false, precision = 6, scale = 2)
  private BigDecimal maximumTemperature;

  @OneToMany(mappedBy = "temperatureUnit", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("measuredAt DESC")
  private final List<TemperatureLog> logs = new ArrayList<>();

  protected TemperatureUnit() {
  }

  /**
   * Creates a temperature unit.
   *
   * @param organization the owning organization
   * @param establishment the owning establishment
   * @param name the unit name
   * @param location the unit location
   * @param type the unit type
   * @param dueByTime the daily deadline for logging a reading
   * @param minimumTemperature the minimum allowed temperature in Celsius
   * @param maximumTemperature the maximum allowed temperature in Celsius
   */
  public TemperatureUnit(
      Organization organization,
      Establishment establishment,
      String name,
      String location,
      TemperatureUnitType type,
      LocalTime dueByTime,
      BigDecimal minimumTemperature,
      BigDecimal maximumTemperature
  ) {
    this.organization = organization;
    this.establishment = establishment;
    this.name = name;
    this.location = location;
    this.type = type;
    this.dueByTime = dueByTime;
    this.minimumTemperature = minimumTemperature;
    this.maximumTemperature = maximumTemperature;
  }

  /**
   * Attaches a temperature log to this unit.
   *
   * @param log the log entry to add
   */
  public void addLog(TemperatureLog log) {
    log.attachTo(this);
    logs.add(log);
  }

  /**
   * Checks whether a temperature falls within the configured allowed range.
   *
   * @param temperatureCelsius the temperature to evaluate
   * @return {@code true} when the temperature is within range
   */
  public boolean isWithinRange(BigDecimal temperatureCelsius) {
    return temperatureCelsius.compareTo(minimumTemperature) >= 0
        && temperatureCelsius.compareTo(maximumTemperature) <= 0;
  }
}
