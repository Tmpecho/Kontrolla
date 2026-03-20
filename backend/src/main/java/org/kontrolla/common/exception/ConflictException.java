package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {

	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}
}
