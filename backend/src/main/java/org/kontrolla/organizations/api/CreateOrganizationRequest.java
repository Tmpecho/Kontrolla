package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.kontrolla.organizations.domain.OrganizationStatus;

/**
 * Request payload for creating an organization.
 *
 * @param name the organization name
 * @param status the initial organization status
 */
public record CreateOrganizationRequest(
    @NotBlank @Size(max = 255) String name, OrganizationStatus status) {}
