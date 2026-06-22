package com.each17.backend.exception;

import com.each17.backend.common.exception.NotFoundException;
import com.each17.backend.common.exception.GlobalExceptionHandler;
import com.each17.backend.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void testHandleResourceNotFound() {
        // Given
        NotFoundException exception = new NotFoundException("Resource not found");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(exception);

        // Then
        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(40401, response.getBody().code());
        assertEquals("Resource not found", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void testHandleValidationExceptions() {
        // Given
        Exception exception = new Exception("Validation error");

        // When
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleUnexpectedException(exception);

        // Then
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(50000, response.getBody().code());
        assertEquals("Internal server error", response.getBody().message());
    }
}
