package org.kontrolla.iam.api;

/**
 * API response returned after successful login or refresh.
 *
 * @param user the authenticated user
 * @param accessToken the issued access token
 * @param tokenType the token type, typically {@code Bearer}
 * @param expiresIn the access-token lifetime in seconds
 * @param appContext the resolved application context
 */
public record LoginResponse(
		AuthUserResponse user,
		String accessToken,
		String tokenType,
		long expiresIn,
		UserAppContextResponse appContext
) {
}
