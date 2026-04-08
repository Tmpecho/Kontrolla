package org.kontrolla.iam.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
		@NotBlank @Size(min = 8, max = 200) String password
) {
}
