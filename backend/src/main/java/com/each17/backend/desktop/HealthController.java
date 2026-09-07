package com.each17.backend.desktop;

import com.each17.backend.common.response.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final String version;

    public HealthController(@Value("${app.version:unknown}") String version) {
        this.version = version;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.success(new HealthResponse("UP", version)));
    }

    public record HealthResponse(String status, String version) {
    }
}
