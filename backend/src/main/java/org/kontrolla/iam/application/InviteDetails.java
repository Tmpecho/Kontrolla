package org.kontrolla.iam.application;

import java.time.Instant;

public record InviteDetails(
		String email,
		String firstName,
		String lastName,
		String organizationName,
		Instant expiresAt
) {
}
