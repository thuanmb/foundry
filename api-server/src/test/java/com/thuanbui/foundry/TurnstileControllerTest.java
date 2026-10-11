package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.controller.TurnstileController;
import com.thuanbui.foundry.model.dto.TurnstileVerifyRequest;
import com.thuanbui.foundry.services.JweTokenService;
import com.thuanbui.foundry.services.TurnstileFailedException;
import com.thuanbui.foundry.services.TurnstileVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurnstileControllerTest {

    @Mock
    private TurnstileVerificationService turnstileService;

    @Mock
    private JweTokenService jweTokenService;

    @Mock
    private HttpServletRequest httpRequest;

    private TurnstileController controller;

    @BeforeEach
    void setUp() {
        controller = new TurnstileController(turnstileService, jweTokenService);
    }

    @Test
    void verify_validToken_shouldReturnJweToken() {
        String testJwe = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIn0..abc.def.ghi.jkl";
        when(turnstileService.verify(eq("valid-token"), anyString())).thenReturn(true);
        when(jweTokenService.generateAnonymousToken(eq("WEB"), any())).thenReturn(testJwe);
        when(jweTokenService.getAnonymousTokenExpiresAt()).thenReturn(Instant.now().plusSeconds(86400));
        when(jweTokenService.hashToken(testJwe)).thenReturn("abc123hash");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        var response = controller.verify(new TurnstileVerifyRequest("valid-token"), httpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(testJwe, response.getBody().jweToken());
        assertNotNull(response.getBody().expiresAt());
        verify(jweTokenService).generateAnonymousToken(eq("WEB"), any());
    }

    @Test
    void verify_invalidToken_shouldThrowException() {
        when(turnstileService.verify(eq("bad-token"), anyString())).thenReturn(false);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThrows(TurnstileFailedException.class, () -> {
            controller.verify(new TurnstileVerifyRequest("bad-token"), httpRequest);
        });
    }

    @Test
    void clientIp_shouldUseXForwardedFor() {
        when(turnstileService.verify(eq("valid-token"), eq("10.0.0.1"))).thenReturn(true);
        when(jweTokenService.generateAnonymousToken(eq("WEB"), any())).thenReturn("token");
        when(jweTokenService.getAnonymousTokenExpiresAt()).thenReturn(Instant.now().plusSeconds(86400));
        when(jweTokenService.hashToken("token")).thenReturn("hash");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");

        controller.verify(new TurnstileVerifyRequest("valid-token"), httpRequest);

        verify(turnstileService).verify("valid-token", "10.0.0.1");
    }
}
