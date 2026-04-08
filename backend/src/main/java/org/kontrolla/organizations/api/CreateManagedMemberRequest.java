package org.kontrolla.organizations.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.kontrolla.organizations.domain.OrganizationRole;

import java.util.List;
import java.util.UUID;
public record CreateManagedMemberRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName,
		@NotNull OrganizationRole role,
		Boolean active,
		Boolean allEstablishments,
		List<UUID> establishmentIds
) {
}
