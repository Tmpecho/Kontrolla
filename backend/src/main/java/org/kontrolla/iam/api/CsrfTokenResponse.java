package org.kontrolla.iam.api;

import org.springframework.security.web.csrf.CsrfToken;

public record CsrfTokenResponse(
		String token,
		String headerName,
		String parameterName
) {

	public static CsrfTokenResponse from(CsrfToken csrfToken) {
		return new CsrfTokenResponse(
				csrfToken.getToken(),
				csrfToken.getHeaderName(),
				csrfToken.getParameterName()
		);
	}
}
