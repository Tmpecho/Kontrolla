package org.kontrolla.iam.application;

import org.kontrolla.iam.domain.User;

/**
 * Authenticated session payload containing issued tokens and resolved app context.
 *
 * @param user the authenticated user
 * @param accessToken the issued access token
 * @param expiresInSeconds access-token lifetime in seconds
 * @param refreshToken the issued refresh token
 * @param appContext the resolved application context for the user
 */
public record AuthSession(
    User user,
    String accessToken,
    long expiresInSeconds,
    String refreshToken,
    UserAppContext appContext) {}
