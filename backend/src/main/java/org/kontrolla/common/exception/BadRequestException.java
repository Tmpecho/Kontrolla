package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

/** Signals that a client request is invalid and should return HTTP 400. */
public class BadRequestException extends ApplicationException {

  /**
   * Creates a bad-request exception with the supplied error code and message.
   *
   * @param code the application-specific error code
   * @param message the human-readable error message
   */
  public BadRequestException(String code, String message) {
    super(HttpStatus.BAD_REQUEST, code, message);
  }
}
