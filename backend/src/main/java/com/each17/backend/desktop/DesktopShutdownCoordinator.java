package com.each17.backend.desktop;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DesktopShutdownCoordinator {
    private static final Duration RESPONSE_GRACE_PERIOD = Duration.ofMillis(150);

    private final ConfigurableApplicationContext applicationContext;

    public void requestShutdown() {
        Thread.ofVirtual().name("desktop-graceful-shutdown").start(() -> {
            try {
                Thread.sleep(RESPONSE_GRACE_PERIOD);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            applicationContext.close();
        });
    }
}
