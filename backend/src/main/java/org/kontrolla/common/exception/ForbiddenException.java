package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

/** Signals that the caller lacks permission to perform an action and should receive HTTP 403. */
public class ForbiddenException extends ApplicationException {

  /**
   * Creates a forbidden exception with the supplied error code and message.
   *
   * @param code the application-specific error code
   * @param message the human-readable error message
   */
  public ForbiddenException(String code, String message) {
    super(HttpStatus.FORBIDDEN, code, message);
  }
}
