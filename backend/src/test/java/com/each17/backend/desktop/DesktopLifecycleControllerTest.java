package com.each17.backend.desktop;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DesktopLifecycleControllerTest {
    @Test
    void acceptsAuthenticatedShutdownRequest() {
        DesktopShutdownCoordinator coordinator = mock(DesktopShutdownCoordinator.class);
        DesktopLifecycleController controller = new DesktopLifecycleController(coordinator, "secret-token");

        assertEquals(HttpStatus.ACCEPTED, controller.shutdown("secret-token").getStatusCode());
        verify(coordinator).requestShutdown();
    }

    @Test
    void rejectsMissingOrInvalidShutdownToken() {
        DesktopShutdownCoordinator coordinator = mock(DesktopShutdownCoordinator.class);
        DesktopLifecycleController controller = new DesktopLifecycleController(coordinator, "secret-token");

        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> controller.shutdown(null));
        ResponseStatusException invalid = assertThrows(ResponseStatusException.class,
                () -> controller.shutdown("wrong-token"));

        assertEquals(HttpStatus.FORBIDDEN, missing.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, invalid.getStatusCode());
        verifyNoInteractions(coordinator);
    }
}
