package org.kontrolla.iam.application;

import org.kontrolla.iam.domain.User;

public record AuthSession(
		User user,
		String accessToken,
		long expiresInSeconds,
		String refreshToken,
		UserAppContext appContext
) {
}
