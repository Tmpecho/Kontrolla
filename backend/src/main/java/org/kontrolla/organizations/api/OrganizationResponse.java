package org.kontrolla.organizations.api;

import java.time.Instant;
import java.util.UUID;
import org.kontrolla.organizations.domain.Organization;
import org.kontrolla.organizations.domain.OrganizationStatus;

/**
 * API response describing an organization.
 *
 * @param id the organization identifier
 * @param name the organization name
 * @param status the organization status
 * @param createdAt when the organization was created
 * @param updatedAt when the organization was last updated
 */
public record OrganizationResponse(
    UUID id, String name, OrganizationStatus status, Instant createdAt, Instant updatedAt) {

  /**
   * Maps an organization entity to the API response shape.
   *
   * @param organization the organization to map
   * @return the mapped response
   */
  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getStatus(),
        organization.getCreatedAt(),
        organization.getUpdatedAt());
  }
}
