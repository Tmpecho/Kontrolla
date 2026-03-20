package org.kontrolla.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;

public final class ApiProblemDetails {

	private ApiProblemDetails() {
	}

	public static ProblemDetail create(HttpStatus status, String code, String message, String path) {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
		detail.setProperty("code", code);
		detail.setProperty("message", message);
		detail.setProperty("path", path);
		detail.setProperty("timestamp", Instant.now());
		return detail;
	}
}
