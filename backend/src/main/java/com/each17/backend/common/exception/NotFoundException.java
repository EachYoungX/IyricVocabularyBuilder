package com.each17.backend.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(40401, HttpStatus.NOT_FOUND, message);
    }
}
