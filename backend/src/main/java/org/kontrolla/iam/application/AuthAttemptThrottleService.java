package org.kontrolla.iam.application;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.springframework.stereotype.Component;

/** In-memory throttling service for login and refresh-token failures. */
@Component
public class AuthAttemptThrottleService {

  private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
  private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Refresh token is invalid";

  private final ConcurrentHashMap<ThrottleKey, FailedAttemptState> failedAttempts =
      new ConcurrentHashMap<>();
  private final AppSecurityProperties securityProperties;

  /**
   * Creates the authentication throttle service.
   *
   * @param securityProperties security properties containing throttle limits
   */
  public AuthAttemptThrottleService(AppSecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  /**
   * Verifies that a login attempt is allowed for the account and client IP.
   *
   * @param accountIdentifier the account identifier, typically email
   * @param clientIp the client IP address
   * @param now the current timestamp
   */
  public void assertLoginAllowed(String accountIdentifier, String clientIp, Instant now) {
    assertAllowed(AuthScope.LOGIN, ThrottleDimension.ACCOUNT, normalize(accountIdentifier), now);
    assertAllowed(AuthScope.LOGIN, ThrottleDimension.IP, normalize(clientIp), now);
  }

  /**
   * Records a failed login attempt for the account and client IP.
   *
   * @param accountIdentifier the account identifier, typically email
   * @param clientIp the client IP address
   * @param now the current timestamp
   */
  public void recordLoginFailure(String accountIdentifier, String clientIp, Instant now) {
    recordFailure(AuthScope.LOGIN, ThrottleDimension.ACCOUNT, normalize(accountIdentifier), now);
    recordFailure(AuthScope.LOGIN, ThrottleDimension.IP, normalize(clientIp), now);
  }

  /**
   * Clears login failure tracking for a successful login.
   *
   * @param accountIdentifier the account identifier, typically email
   * @param clientIp the client IP address
   */
  public void resetLogin(String accountIdentifier, String clientIp) {
    reset(AuthScope.LOGIN, ThrottleDimension.ACCOUNT, normalize(accountIdentifier));
    reset(AuthScope.LOGIN, ThrottleDimension.IP, normalize(clientIp));
  }

  /**
   * Verifies that a refresh-token attempt is allowed for the account and client IP.
   *
   * @param accountIdentifier the account identifier, if known
   * @param clientIp the client IP address
   * @param now the current timestamp
   */
  public void assertRefreshAllowed(String accountIdentifier, String clientIp, Instant now) {
    if (accountIdentifier != null && !accountIdentifier.isBlank()) {
      assertAllowed(
          AuthScope.REFRESH, ThrottleDimension.ACCOUNT, normalize(accountIdentifier), now);
    }
    assertAllowed(AuthScope.REFRESH, ThrottleDimension.IP, normalize(clientIp), now);
  }

  /**
   * Records a failed refresh attempt scoped only to client IP.
   *
   * @param clientIp the client IP address
   * @param now the current timestamp
   */
  public void recordRefreshFailure(String clientIp, Instant now) {
    recordFailure(AuthScope.REFRESH, ThrottleDimension.IP, normalize(clientIp), now);
  }

  /**
   * Records a failed refresh attempt for the account and client IP.
   *
   * @param accountIdentifier the account identifier
   * @param clientIp the client IP address
   * @param now the current timestamp
   */
  public void recordRefreshFailure(String accountIdentifier, String clientIp, Instant now) {
    recordFailure(AuthScope.REFRESH, ThrottleDimension.ACCOUNT, normalize(accountIdentifier), now);
    recordFailure(AuthScope.REFRESH, ThrottleDimension.IP, normalize(clientIp), now);
  }

  /**
   * Clears refresh failure tracking for a successful refresh.
   *
   * @param accountIdentifier the account identifier
   * @param clientIp the client IP address
   */
  public void resetRefresh(String accountIdentifier, String clientIp) {
    reset(AuthScope.REFRESH, ThrottleDimension.ACCOUNT, normalize(accountIdentifier));
    reset(AuthScope.REFRESH, ThrottleDimension.IP, normalize(clientIp));
  }

  /** Clears all recorded throttle state. */
  public void clear() {
    failedAttempts.clear();
  }

  private void assertAllowed(
      AuthScope scope, ThrottleDimension dimension, String key, Instant now) {
    FailedAttemptState state = failedAttempts.get(new ThrottleKey(scope, dimension, key));

    if (state == null) {
      return;
    }

    if (state.lockedUntil() != null && now.isBefore(state.lockedUntil())) {
      throw unauthorizedException(scope, dimension);
    }

    if (state.lockedUntil() != null && !now.isBefore(state.lockedUntil())) {
      failedAttempts.remove(new ThrottleKey(scope, dimension, key), state);
    }
  }

  private void recordFailure(
      AuthScope scope, ThrottleDimension dimension, String key, Instant now) {
    ThrottleKey throttleKey = new ThrottleKey(scope, dimension, key);

    failedAttempts.compute(
        throttleKey,
        (ignored, currentState) -> {
          FailedAttemptState activeState = currentState;

          if (activeState == null
              || (activeState.lockedUntil() != null && !now.isBefore(activeState.lockedUntil()))) {
            activeState = new FailedAttemptState(0, null);
          }

          int failedAttemptCount = activeState.failedAttemptCount() + 1;
          Instant lockedUntil =
              failedAttemptCount >= securityProperties.getAuthThrottling().getMaxFailedAttempts()
                  ? now.plus(securityProperties.getAuthThrottling().getLockoutDuration())
                  : null;

          return new FailedAttemptState(failedAttemptCount, lockedUntil);
        });
  }

  private void reset(AuthScope scope, ThrottleDimension dimension, String key) {
    failedAttempts.remove(new ThrottleKey(scope, dimension, key));
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private AuthThrottleException unauthorizedException(
      AuthScope scope, ThrottleDimension dimension) {
    // Keep throttle failures indistinguishable from normal auth failures so we do not disclose
    // lockout state.
    return scope == AuthScope.LOGIN
        ? new AuthThrottleException(
            "invalid_credentials", INVALID_CREDENTIALS_MESSAGE, dimension.name())
        : new AuthThrottleException(
            "invalid_refresh_token", INVALID_REFRESH_TOKEN_MESSAGE, dimension.name());
  }

  private enum AuthScope {
    LOGIN,
    REFRESH
  }

  private enum ThrottleDimension {
    ACCOUNT,
    IP
  }

  private record ThrottleKey(AuthScope scope, ThrottleDimension dimension, String key) {}

  private record FailedAttemptState(int failedAttemptCount, Instant lockedUntil) {}
}
