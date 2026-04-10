package org.kontrolla.organizations.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.kontrolla.organizations.domain.OrganizationRole;

/**
 * Request payload for creating an organization membership for an existing user.
 *
 * @param userId the user identifier
 * @param role the organization role
 * @param active whether the membership should be active
 * @param allEstablishments whether all-establishment access should be granted
 * @param establishmentIds explicit establishment scope, if any
 */
public record CreateMembershipRequest(
    @NotNull UUID userId,
    @NotNull OrganizationRole role,
    Boolean active,
    Boolean allEstablishments,
    List<UUID> establishmentIds) {}
