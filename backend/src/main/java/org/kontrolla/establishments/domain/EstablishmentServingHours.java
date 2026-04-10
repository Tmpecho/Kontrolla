package org.kontrolla.establishments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.kontrolla.common.persistence.AbstractAuditableUuidEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;

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

  protected EstablishmentServingHours() {
  }

  public EstablishmentServingHours(
      Establishment establishment,
      DayOfWeek dayOfWeek,
      boolean closed,
      LocalTime opensAt,
      LocalTime closesAt
  ) {
    this.establishment = establishment;
    this.dayOfWeek = dayOfWeek;
    this.closed = closed;
    this.opensAt = closed ? null : opensAt;
    this.closesAt = closed ? null : closesAt;
  }

  public void update(boolean closed, LocalTime opensAt, LocalTime closesAt) {
    this.closed = closed;
    this.opensAt = closed ? null : opensAt;
    this.closesAt = closed ? null : closesAt;
  }
}
