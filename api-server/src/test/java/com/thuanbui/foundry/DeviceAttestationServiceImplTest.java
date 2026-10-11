package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.config.AttestationProperties;
import com.thuanbui.foundry.services.AndroidPlayIntegrityService;
import com.thuanbui.foundry.services.DeviceAttestationServiceImpl;
import com.thuanbui.foundry.services.IosAppAttestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceAttestationServiceImplTest {

    @Mock
    private IosAppAttestService iosAppAttestService;

    @Mock
    private AndroidPlayIntegrityService androidPlayIntegrityService;

    @Mock
    private AttestationProperties properties;

    @InjectMocks
    private DeviceAttestationServiceImpl attestationService;

    @Test
    void verify_shouldBypass_whenEnabled() {
        when(properties.bypassEnabled()).thenReturn(true);

        assertTrue(attestationService.verify("any-token", "IOS"));
        verify(iosAppAttestService, never()).verify(anyString());
    }

    @Test
    void verify_shouldDelegateToIos_whenPlatformIsIOS() {
        when(properties.bypassEnabled()).thenReturn(false);
        when(iosAppAttestService.verify("ios-token")).thenReturn(true);

        assertTrue(attestationService.verify("ios-token", "IOS"));
    }

    @Test
    void verify_shouldDelegateToAndroid_whenPlatformIsANDROID() {
        when(properties.bypassEnabled()).thenReturn(false);
        when(androidPlayIntegrityService.verify("android-token")).thenReturn(true);

        assertTrue(attestationService.verify("android-token", "ANDROID"));
    }

    @Test
    void verify_shouldRejectInvalidPlatform() {
        when(properties.bypassEnabled()).thenReturn(false);

        assertFalse(attestationService.verify("token", "UNKNOWN"));
    }

    @Test
    void verify_shouldFailClosed_whenIosServiceReturnsFalse() {
        when(properties.bypassEnabled()).thenReturn(false);
        when(iosAppAttestService.verify("bad-token")).thenReturn(false);

        assertFalse(attestationService.verify("bad-token", "IOS"));
    }

    @Test
    void verify_shouldFailClosed_whenAndroidServiceReturnsFalse() {
        when(properties.bypassEnabled()).thenReturn(false);
        when(androidPlayIntegrityService.verify("bad-token")).thenReturn(false);

        assertFalse(attestationService.verify("bad-token", "ANDROID"));
    }
}
