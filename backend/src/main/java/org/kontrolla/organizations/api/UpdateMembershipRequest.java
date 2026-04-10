package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotNull;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for updating an organization membership.
 *
 * @param role the organization role
 * @param active whether the membership is active
 * @param allEstablishments whether all-establishment access should be granted
 * @param establishmentIds explicit establishment scope, if any
 */
public record UpdateMembershipRequest(
		@NotNull OrganizationRole role,
		@NotNull Boolean active,
		Boolean allEstablishments,
		List<UUID> establishmentIds
) {
}
