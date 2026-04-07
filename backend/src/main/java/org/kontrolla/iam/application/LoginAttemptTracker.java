package org.kontrolla.iam.application;

import org.kontrolla.common.exception.UnauthorizedException;
import org.kontrolla.iam.security.AppSecurityProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptTracker {

	private final ConcurrentHashMap<String, FailedLoginState> failedAttemptsByIdentifier = new ConcurrentHashMap<>();
	private final AppSecurityProperties securityProperties;

	public LoginAttemptTracker(AppSecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

	public void assertLoginAllowed(String identifier, Instant now) {
		FailedLoginState state = failedAttemptsByIdentifier.get(normalizeIdentifier(identifier));

		if (state == null) {
			return;
		}

		if (state.lockedUntil() != null && now.isBefore(state.lockedUntil())) {
			throw new UnauthorizedException("invalid_credentials", "Invalid email or password");
		}

		if (state.lockedUntil() != null && !now.isBefore(state.lockedUntil())) {
			failedAttemptsByIdentifier.remove(normalizeIdentifier(identifier), state);
		}
	}

	public void recordFailedAttempt(String identifier, Instant now) {
		String normalizedIdentifier = normalizeIdentifier(identifier);

		failedAttemptsByIdentifier.compute(normalizedIdentifier, (ignoredKey, currentState) -> {
			FailedLoginState activeState = currentState;

			if (activeState == null || (activeState.lockedUntil() != null && !now.isBefore(activeState.lockedUntil()))) {
				activeState = new FailedLoginState(0, null);
			}

			int failedAttemptCount = activeState.failedAttemptCount() + 1;
			Instant lockedUntil = failedAttemptCount >= securityProperties.getLogin().getMaxFailedAttempts()
					? now.plus(securityProperties.getLogin().getLockoutDuration())
					: null;

			return new FailedLoginState(failedAttemptCount, lockedUntil);
		});
	}

	public void reset(String identifier) {
		failedAttemptsByIdentifier.remove(normalizeIdentifier(identifier));
	}

	public void clear() {
		failedAttemptsByIdentifier.clear();
	}

	private String normalizeIdentifier(String identifier) {
		return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
	}

	private record FailedLoginState(int failedAttemptCount, Instant lockedUntil) {
	}

}
