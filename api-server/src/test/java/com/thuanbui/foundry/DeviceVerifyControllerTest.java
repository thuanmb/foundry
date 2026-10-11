package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.controller.DeviceVerifyController;
import com.thuanbui.foundry.model.dto.DeviceVerifyRequest;
import com.thuanbui.foundry.services.AttestationFailedException;
import com.thuanbui.foundry.services.DeviceAttestationService;
import com.thuanbui.foundry.services.JweTokenService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceVerifyControllerTest {

    @Mock
    private DeviceAttestationService attestationService;
    @Mock
    private JweTokenService jweTokenService;
    private DeviceVerifyController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceVerifyController(attestationService, jweTokenService);
    }

    @Test
    void verify_validAttestation_shouldReturnJweToken() {
        String testJwe = "eyJhbGciOiJBMjU2S1ciLCJlbmMiOiJBMjU2R0NNIn0..abc.def.ghi.jkl";
        when(attestationService.verify(eq("valid-attestation"), eq("IOS"))).thenReturn(true);
        when(jweTokenService.generateAnonymousToken(eq("MOBILE"), any())).thenReturn(testJwe);
        when(jweTokenService.getAnonymousTokenExpiresAt()).thenReturn(Instant.now().plusSeconds(86400));
        when(jweTokenService.hashToken(testJwe)).thenReturn("abc123hash");

        var response = controller.verify(new DeviceVerifyRequest("valid-attestation", "IOS"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(testJwe, response.getBody().jweToken());
        assertNotNull(response.getBody().expiresAt());
        verify(jweTokenService).generateAnonymousToken(eq("MOBILE"), any());
    }

    @Test
    void verify_invalidAttestation_shouldThrowException() {
        when(attestationService.verify(eq("bad-attestation"), eq("ANDROID"))).thenReturn(false);

        assertThrows(AttestationFailedException.class, () -> {
            controller.verify(new DeviceVerifyRequest("bad-attestation", "ANDROID"));
        });
    }
}
