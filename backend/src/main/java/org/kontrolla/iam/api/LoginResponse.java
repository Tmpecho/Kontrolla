package org.kontrolla.iam.api;

public record LoginResponse(
		AuthUserResponse user,
		String accessToken,
		String tokenType,
		long expiresIn,
		UserAppContextResponse appContext
) {
}
