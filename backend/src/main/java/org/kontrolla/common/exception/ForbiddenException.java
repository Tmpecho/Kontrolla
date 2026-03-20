package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApplicationException {

	public ForbiddenException(String code, String message) {
		super(HttpStatus.FORBIDDEN, code, message);
	}
}
