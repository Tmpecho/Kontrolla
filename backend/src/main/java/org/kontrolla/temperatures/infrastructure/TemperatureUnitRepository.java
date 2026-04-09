package org.kontrolla.temperatures.infrastructure;

import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TemperatureUnitRepository extends JpaRepository<TemperatureUnit, UUID> {

  @EntityGraph(attributePaths = {
      "organization",
      "establishment",
      "logs",
      "logs.loggedByUser"
  })
  List<TemperatureUnit> findByEstablishmentIdAndOrganizationIdOrderByNameAsc(
      UUID establishmentId,
      UUID organizationId
  );

  @EntityGraph(attributePaths = {
      "organization",
      "establishment"
  })
  Optional<TemperatureUnit> findByIdAndEstablishmentIdAndOrganizationId(
      UUID id,
      UUID establishmentId,
      UUID organizationId
  );
}
