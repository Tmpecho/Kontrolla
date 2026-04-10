package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Signals that authentication is required or invalid and should return
 * HTTP 401.
 */
public class UnauthorizedException extends ApplicationException {

	/**
	 * Creates an unauthorized exception with the supplied error code and message.
	 *
	 * @param code the application-specific error code
	 * @param message the human-readable error message
	 */
	public UnauthorizedException(String code, String message) {
		super(HttpStatus.UNAUTHORIZED, code, message);
	}
}
