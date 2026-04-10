package org.kontrolla.establishments.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.kontrolla.establishments.domain.EstablishmentServingHours;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for establishment serving-hours records. */
public interface EstablishmentServingHoursRepository
    extends JpaRepository<EstablishmentServingHours, UUID> {

  /**
   * Returns serving-hours records for an establishment ordered by weekday.
   *
   * @param establishmentId the establishment identifier
   * @return the serving-hours records
   */
  List<EstablishmentServingHours> findByEstablishmentIdOrderByDayOfWeekAsc(UUID establishmentId);

  /**
   * Finds serving-hours configuration for a specific establishment weekday.
   *
   * @param establishmentId the establishment identifier
   * @param dayOfWeek the weekday to look up
   * @return the matching serving-hours record, if present
   */
  Optional<EstablishmentServingHours> findByEstablishmentIdAndDayOfWeek(
      UUID establishmentId, java.time.DayOfWeek dayOfWeek);
}
