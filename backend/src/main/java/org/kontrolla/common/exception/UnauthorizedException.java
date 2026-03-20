package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApplicationException {

	public UnauthorizedException(String code, String message) {
		super(HttpStatus.UNAUTHORIZED, code, message);
	}
}
