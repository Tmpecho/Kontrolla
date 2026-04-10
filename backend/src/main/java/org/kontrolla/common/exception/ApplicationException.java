package org.kontrolla.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base runtime exception for application errors that should be translated into structured HTTP
 * responses.
 */
@Getter
public class ApplicationException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  /**
   * Creates an application exception with the HTTP status and error code that should be exposed by
   * the API layer.
   *
   * @param status the HTTP status associated with the failure
   * @param code the application-specific error code
   * @param message the human-readable error message
   */
  public ApplicationException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}
