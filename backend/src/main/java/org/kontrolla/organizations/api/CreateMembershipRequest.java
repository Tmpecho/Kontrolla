package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.UUID;

public record CreateMembershipRequest(
		@NotNull UUID userId,
		@NotNull OrganizationRole role,
		Boolean active
) {
}
