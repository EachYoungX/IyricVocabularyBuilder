package com.each17.backend.common.exception;

import org.springframework.http.HttpStatus;

public class DictionaryNotFoundException extends BusinessException {
    public DictionaryNotFoundException(String word) {
        super(40402, HttpStatus.NOT_FOUND, "Dictionary entry not found: " + word);
    }
}
