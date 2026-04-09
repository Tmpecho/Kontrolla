package org.kontrolla.iam.application;

import java.time.Instant;

public record IssuedInvite(
		Instant expiresAt,
		String inviteUrl
) {
}
