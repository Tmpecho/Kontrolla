package org.kontrolla.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
		@NotBlank @Size(max = 255) String firstName,
		@NotBlank @Size(max = 255) String lastName
) {
}
