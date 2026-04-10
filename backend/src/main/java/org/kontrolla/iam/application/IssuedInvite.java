package org.kontrolla.iam.application;

import java.time.Instant;

/**
 * Result of issuing a user invite.
 *
 * @param expiresAt when the invite expires
 * @param inviteUrl the invite URL exposed to the caller, if configured
 */
public record IssuedInvite(
		Instant expiresAt,
		String inviteUrl
) {
}
