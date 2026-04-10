package org.kontrolla.organizations.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.kontrolla.organizations.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for organization persistence and name-based lookup. */
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

  /**
   * Finds an organization by case-insensitive name.
   *
   * @param name the organization name
   * @return the matching organization, if present
   */
  Optional<Organization> findByNameIgnoreCase(String name);
}
