package org.kontrolla.iam.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.kontrolla.iam.domain.GlobalRole;

import java.util.Set;

/**
 * Request payload for creating a user administratively.
 *
 * @param email the user email
 * @param firstName the user first name
 * @param lastName the user last name
 * @param password the user password
 * @param active whether the user should be active
 * @param globalRoles the user's global roles
 */
public record CreateUserRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName,
		@NotBlank @Size(min = 8, max = 200) String password,
		Boolean active,
		Set<GlobalRole> globalRoles
) {
}
