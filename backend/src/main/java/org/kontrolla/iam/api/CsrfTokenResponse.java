package org.kontrolla.iam.api;

import org.springframework.security.web.csrf.CsrfToken;

/**
 * API response exposing a CSRF token and its header and parameter names.
 *
 * @param token the CSRF token value
 * @param headerName the expected header name
 * @param parameterName the expected parameter name
 */
public record CsrfTokenResponse(String token, String headerName, String parameterName) {

  /**
   * Maps a Spring CSRF token to the API response shape.
   *
   * @param csrfToken the CSRF token to map
   * @return the mapped response
   */
  public static CsrfTokenResponse from(CsrfToken csrfToken) {
    return new CsrfTokenResponse(
        csrfToken.getToken(), csrfToken.getHeaderName(), csrfToken.getParameterName());
  }
}
