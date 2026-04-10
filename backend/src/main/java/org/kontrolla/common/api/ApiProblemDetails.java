package org.kontrolla.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;

/**
 * Utility for creating consistently structured RFC 7807 problem detail
 * responses for API errors.
 */
public final class ApiProblemDetails {

	private ApiProblemDetails() {
	}

	/**
	 * Creates a problem detail payload with shared metadata used across API error
	 * responses.
	 *
	 * @param status the HTTP status to expose
	 * @param code the application-specific error code
	 * @param message the human-readable error message
	 * @param path the request path that produced the error
	 * @return a populated problem detail response
	 */
	public static ProblemDetail create(HttpStatus status, String code, String message, String path) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
		detail.setProperty("code", code);
		detail.setProperty("message", message);
		detail.setProperty("path", path);
		detail.setProperty("timestamp", Instant.now());
		return detail;
	}
}
