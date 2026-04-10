package org.kontrolla.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for changing the current user's password.
 *
 * @param currentPassword the current password
 * @param newPassword the replacement password
 */
public record ChangeMyPasswordRequest(
		@NotBlank @Size(min = 8, max = 200) String currentPassword,
		@NotBlank @Size(min = 8, max = 200) String newPassword
) {
}
