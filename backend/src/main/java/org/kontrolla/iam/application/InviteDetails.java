package org.kontrolla.iam.application;

import java.time.Instant;

/**
 * Public invite details exposed before an invitation is accepted.
 *
 * @param email the invited email address
 * @param firstName the invited user's first name
 * @param lastName the invited user's last name
 * @param organizationName the invited organization name
 * @param expiresAt when the invitation expires
 */
public record InviteDetails(
		String email,
		String firstName,
		String lastName,
		String organizationName,
		Instant expiresAt
) {
}
