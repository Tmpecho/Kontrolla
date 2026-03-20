package org.kontrolla.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.kontrolla.common.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(ApplicationException.class)
	public ResponseEntity<ProblemDetail> handleApplicationException(
			ApplicationException exception,
			HttpServletRequest request
	) {
		return ResponseEntity
				.status(exception.getStatus())
				.body(ApiProblemDetails.create(exception.getStatus(), exception.getCode(), exception.getMessage(), request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		ProblemDetail detail = ApiProblemDetails.create(HttpStatus.BAD_REQUEST, "validation_failed", message, request.getRequestURI());
		return ResponseEntity.badRequest().body(detail);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ProblemDetail> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		String message = exception.getConstraintViolations()
				.stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.collect(Collectors.joining(", "));
		ProblemDetail detail = ApiProblemDetails.create(HttpStatus.BAD_REQUEST, "validation_failed", message, request.getRequestURI());
		return ResponseEntity.badRequest().body(detail);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(
			MethodArgumentTypeMismatchException exception,
			HttpServletRequest request
	) {
		String message = "Invalid value for '" + exception.getName() + "'";
		ProblemDetail detail = ApiProblemDetails.create(HttpStatus.BAD_REQUEST, "invalid_argument", message, request.getRequestURI());
		return ResponseEntity.badRequest().body(detail);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ProblemDetail> handleAccessDenied(
			AccessDeniedException exception,
			HttpServletRequest request
	) {
		ProblemDetail detail = ApiProblemDetails.create(HttpStatus.FORBIDDEN, "access_denied", "Access denied", request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception", exception);
		ProblemDetail detail = ApiProblemDetails.create(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"internal_error",
				"An unexpected error occurred",
				request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
	}
}
