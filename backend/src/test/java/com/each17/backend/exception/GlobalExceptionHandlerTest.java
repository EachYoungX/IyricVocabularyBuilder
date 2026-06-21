package com.each17.backend.exception;

import com.each17.backend.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void testHandleResourceNotFound() {
        // Given
        RuntimeException exception = new RuntimeException("Resource not found");

        // When
        ErrorResponseDto response = exceptionHandler.handleResourceNotFound(exception);

        // Then
        assertEquals("NOT_FOUND", response.getError());
        assertEquals("Resource not found", response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void testHandleValidationExceptions() {
        // Given
        Exception exception = new Exception("Validation error");

        // When
        ErrorResponseDto response = exceptionHandler.handleValidationExceptions(exception);

        // Then
        assertEquals("BadRequest", response.getError());
        assertEquals("Validation error", response.getMessage());
        assertNotNull(response.getTimestamp());
    }
}