package org.kontrolla.temperatures.infrastructure;

import org.kontrolla.temperatures.domain.TemperatureUnit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for temperature units and their associated readings.
 */
public interface TemperatureUnitRepository extends JpaRepository<TemperatureUnit, UUID> {

  /**
   * Returns temperature units for an establishment ordered by name with
   * associated data loaded eagerly.
   *
   * @param establishmentId the establishment identifier
   * @param organizationId the organization identifier
   * @return the matching temperature units
   */
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

  /**
   * Finds a temperature unit by id scoped to an establishment and organization.
   *
   * @param id the temperature unit identifier
   * @param establishmentId the establishment identifier
   * @param organizationId the organization identifier
   * @return the matching temperature unit, if present
   */
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
