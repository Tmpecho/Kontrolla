package org.kontrolla.common.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
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

/** Translates application and framework exceptions into consistent API error responses. */
@RestControllerAdvice
public class RestExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  /**
   * Converts a domain-level application exception into its configured HTTP response.
   *
   * @param exception the application exception to render
   * @param request the originating HTTP request
   * @return a problem detail response matching the exception status and code
   */
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ProblemDetail> handleApplicationException(
      ApplicationException exception, HttpServletRequest request) {
    return ResponseEntity.status(exception.getStatus())
        .body(
            ApiProblemDetails.create(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI()));
  }

  /**
   * Aggregates bean validation field errors into a single bad-request response.
   *
   * @param exception the validation failure raised during request binding
   * @param request the originating HTTP request
   * @return a bad-request problem detail containing the validation errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    ProblemDetail detail =
        ApiProblemDetails.create(
            HttpStatus.BAD_REQUEST, "validation_failed", message, request.getRequestURI());
    return ResponseEntity.badRequest().body(detail);
  }

  /**
   * Converts constraint violations outside request-body binding into a bad-request response.
   *
   * @param exception the constraint violation exception
   * @param request the originating HTTP request
   * @return a bad-request problem detail containing the violation messages
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(
      ConstraintViolationException exception, HttpServletRequest request) {
    String message =
        exception.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", "));
    ProblemDetail detail =
        ApiProblemDetails.create(
            HttpStatus.BAD_REQUEST, "validation_failed", message, request.getRequestURI());
    return ResponseEntity.badRequest().body(detail);
  }

  /**
   * Handles request arguments that cannot be converted to the expected type.
   *
   * @param exception the type mismatch exception
   * @param request the originating HTTP request
   * @return a bad-request problem detail describing the invalid argument
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    String message = "Invalid value for '" + exception.getName() + "'";
    ProblemDetail detail =
        ApiProblemDetails.create(
            HttpStatus.BAD_REQUEST, "invalid_argument", message, request.getRequestURI());
    return ResponseEntity.badRequest().body(detail);
  }

  /**
   * Converts security authorization failures into a forbidden response.
   *
   * @param exception the access denied exception
   * @param request the originating HTTP request
   * @return a forbidden problem detail response
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDenied(
      AccessDeniedException exception, HttpServletRequest request) {
    ProblemDetail detail =
        ApiProblemDetails.create(
            HttpStatus.FORBIDDEN, "access_denied", "Access denied", request.getRequestURI());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
  }

  /**
   * Fallback handler for unexpected exceptions that were not matched by more specific handlers.
   *
   * @param exception the unexpected exception
   * @param request the originating HTTP request
   * @return an internal-server-error problem detail response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("Unhandled exception", exception);
    ProblemDetail detail =
        ApiProblemDetails.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal_error",
            "An unexpected error occurred",
            request.getRequestURI());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
  }
}
