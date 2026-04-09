package org.kontrolla.establishments.infrastructure;

import org.kontrolla.establishments.domain.EstablishmentServingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstablishmentServingHoursRepository extends JpaRepository<EstablishmentServingHours, UUID> {

  List<EstablishmentServingHours> findByEstablishmentIdOrderByDayOfWeekAsc(UUID establishmentId);

  Optional<EstablishmentServingHours> findByEstablishmentIdAndDayOfWeek(
      UUID establishmentId,
      java.time.DayOfWeek dayOfWeek
  );
}
