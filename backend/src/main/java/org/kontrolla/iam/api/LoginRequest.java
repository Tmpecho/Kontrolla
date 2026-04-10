package org.kontrolla.iam.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for logging in with email and password.
 *
 * @param email the login email
 * @param password the login password
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 8, max = 200) String password
) {
}
