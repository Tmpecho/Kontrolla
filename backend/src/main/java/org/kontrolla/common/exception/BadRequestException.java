package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApplicationException {

	public BadRequestException(String code, String message) {
		super(HttpStatus.BAD_REQUEST, code, message);
	}
}
