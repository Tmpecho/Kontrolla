package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.kontrolla.organizations.domain.OrganizationStatus;

public record CreateOrganizationRequest(
		@NotBlank @Size(max = 255) String name,
		OrganizationStatus status
) {
}
