package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Signals that a request conflicts with current application state and should
 * return HTTP 409.
 */
public class ConflictException extends ApplicationException {

	/**
	 * Creates a conflict exception with the supplied error code and message.
	 *
	 * @param code the application-specific error code
	 * @param message the human-readable error message
	 */
	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}
}
