package com.each17.backend.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(40901, HttpStatus.CONFLICT, message);
    }
}
