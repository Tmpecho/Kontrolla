package org.kontrolla.organizations.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.kontrolla.organizations.domain.OrganizationRole;

/**
 * Request payload for creating a managed user together with an organization membership.
 *
 * @param email the invited user email
 * @param firstName the invited user first name
 * @param lastName the invited user last name
 * @param role the organization role
 * @param active whether the membership should be active
 * @param allEstablishments whether all-establishment access should be granted
 * @param establishmentIds explicit establishment scope, if any
 */
public record CreateManagedMemberRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 255) String firstName,
    @NotBlank @Size(max = 255) String lastName,
    @NotNull OrganizationRole role,
    Boolean active,
    Boolean allEstablishments,
    List<UUID> establishmentIds) {}
