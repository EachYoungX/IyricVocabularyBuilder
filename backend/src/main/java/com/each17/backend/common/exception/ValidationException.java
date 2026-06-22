package com.each17.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(40001, HttpStatus.BAD_REQUEST, message);
    }
}
