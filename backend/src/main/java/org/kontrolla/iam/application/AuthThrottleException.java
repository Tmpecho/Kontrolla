package org.kontrolla.iam.application;

import org.kontrolla.common.exception.UnauthorizedException;

final class AuthThrottleException extends UnauthorizedException {

	private final String throttleDimension;

	AuthThrottleException(String code, String message, String throttleDimension) {
		super(code, message);
		this.throttleDimension = throttleDimension;
	}

	String getThrottleDimension() {
		return throttleDimension;
	}
}
