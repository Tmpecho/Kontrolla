package org.kontrolla.establishments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

/** Persisted serving-hours configuration for a single establishment weekday. */
@Getter
@Entity
@Table(name = "establishment_serving_hours")
public class EstablishmentServingHours extends AbstractAuditableUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "establishment_id", nullable = false)
  private Establishment establishment;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 16)
  private DayOfWeek dayOfWeek;

  @Column(name = "is_closed", nullable = false)
  private boolean closed;

  @Column(name = "opens_at")
  private LocalTime opensAt;

  @Column(name = "closes_at")
  private LocalTime closesAt;

  protected EstablishmentServingHours() {}

  /**
   * Creates serving-hours configuration for a specific weekday.
   *
   * @param establishment the owning establishment
   * @param dayOfWeek the weekday represented
   * @param closed whether the establishment is closed that day
   * @param opensAt the opening time, if open
   * @param closesAt the closing time, if open
   */
  public EstablishmentServingHours(
      Establishment establishment,
      DayOfWeek dayOfWeek,
      boolean closed,
      LocalTime opensAt,
      LocalTime closesAt) {
    this.establishment = establishment;
    this.dayOfWeek = dayOfWeek;
    this.closed = closed;
    this.opensAt = closed ? null : opensAt;
    this.closesAt = closed ? null : closesAt;
  }

  /**
   * Updates the serving-hours state for the weekday.
   *
   * @param closed whether the establishment is closed that day
   * @param opensAt the opening time, if open
   * @param closesAt the closing time, if open
   */
  public void update(boolean closed, LocalTime opensAt, LocalTime closesAt) {
    this.closed = closed;
    this.opensAt = closed ? null : opensAt;
    this.closesAt = closed ? null : closesAt;
  }
}
