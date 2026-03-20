package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.organizations.domain.OrganizationRole;

public record UpdateMembershipRequest(
		@NotNull OrganizationRole role,
		@NotNull Boolean active
) {
}
