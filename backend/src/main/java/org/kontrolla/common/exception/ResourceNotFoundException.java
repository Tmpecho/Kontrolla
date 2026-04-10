package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Signals that a requested resource could not be found and should return
 * HTTP 404.
 */
public class ResourceNotFoundException extends ApplicationException {

	/**
	 * Creates a not-found exception with the supplied error code and message.
	 *
	 * @param code the application-specific error code
	 * @param message the human-readable error message
	 */
	public ResourceNotFoundException(String code, String message) {
		super(HttpStatus.NOT_FOUND, code, message);
	}
}
