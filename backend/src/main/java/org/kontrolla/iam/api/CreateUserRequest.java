package org.kontrolla.iam.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.kontrolla.iam.domain.GlobalRole;

import java.util.Set;

public record CreateUserRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName,
		@NotBlank @Size(min = 8, max = 200) String password,
		Boolean active,
		Set<GlobalRole> globalRoles
) {
}
