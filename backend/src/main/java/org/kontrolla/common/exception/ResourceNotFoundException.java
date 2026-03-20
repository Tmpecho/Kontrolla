package org.kontrolla.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationException {

	public ResourceNotFoundException(String code, String message) {
		super(HttpStatus.NOT_FOUND, code, message);
	}
}
