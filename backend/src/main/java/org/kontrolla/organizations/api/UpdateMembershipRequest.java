package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.List;
import java.util.UUID;

public record UpdateMembershipRequest(
		@NotNull OrganizationRole role,
		@NotNull Boolean active,
		Boolean allEstablishments,
		List<UUID> establishmentIds
) {
}
