package com.each17.backend.desktop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/desktop")
@ConditionalOnProperty(name = "app.desktop.mode", havingValue = "true")
public class DesktopLifecycleController {
    static final String SHUTDOWN_TOKEN_HEADER = "X-Desktop-Shutdown-Token";

    private final DesktopShutdownCoordinator shutdownCoordinator;
    private final String shutdownToken;

    public DesktopLifecycleController(
            DesktopShutdownCoordinator shutdownCoordinator,
            @Value("${app.desktop.shutdown-token:}") String shutdownToken
    ) {
        this.shutdownCoordinator = shutdownCoordinator;
        this.shutdownToken = shutdownToken;
    }

    @PostMapping("/shutdown")
    public ResponseEntity<Void> shutdown(
            @RequestHeader(name = SHUTDOWN_TOKEN_HEADER, required = false) String suppliedToken
    ) {
        if (!validToken(suppliedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid desktop shutdown token");
        }
        shutdownCoordinator.requestShutdown();
        return ResponseEntity.accepted().build();
    }

    private boolean validToken(String suppliedToken) {
        if (shutdownToken.isBlank() || suppliedToken == null) return false;
        return MessageDigest.isEqual(
                shutdownToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
