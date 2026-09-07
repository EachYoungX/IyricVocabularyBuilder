package com.each17.backend.desktop;

import com.each17.backend.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {
    @Test
    void reportsReadyWithApplicationVersion() {
        ResponseEntity<ApiResponse<HealthController.HealthResponse>> response =
                new HealthController("1.0.0").health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().data().status());
        assertEquals("1.0.0", response.getBody().data().version());
    }
}
