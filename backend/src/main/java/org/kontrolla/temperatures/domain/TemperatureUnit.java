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

  public void addLog(TemperatureLog log) {
    log.attachTo(this);
    logs.add(log);
  }

  public boolean isWithinRange(BigDecimal temperatureCelsius) {
    return temperatureCelsius.compareTo(minimumTemperature) >= 0
        && temperatureCelsius.compareTo(maximumTemperature) <= 0;
  }
}
