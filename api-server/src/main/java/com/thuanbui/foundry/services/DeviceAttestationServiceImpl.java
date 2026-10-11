package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.AttestationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DeviceAttestationServiceImpl implements DeviceAttestationService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAttestationServiceImpl.class);

    private final IosAppAttestService iosAppAttestService;
    private final AndroidPlayIntegrityService androidPlayIntegrityService;
    private final AttestationProperties properties;

    public DeviceAttestationServiceImpl(IosAppAttestService iosAppAttestService,
            AndroidPlayIntegrityService androidPlayIntegrityService,
            AttestationProperties properties) {
        this.iosAppAttestService = iosAppAttestService;
        this.androidPlayIntegrityService = androidPlayIntegrityService;
        this.properties = properties;
    }

    @Override
    public boolean verify(String attestationToken, String platform) {
        if (properties.bypassEnabled()) {
            log.info("Device attestation bypassed: platform={}", platform);
            return true;
        }

        return switch (platform.toUpperCase()) {
            case "IOS" -> iosAppAttestService.verify(attestationToken);
            case "ANDROID" -> androidPlayIntegrityService.verify(attestationToken);
            default -> {
                log.warn("Unknown attestation platform: {}", platform);
                yield false;
            }
        };
    }
}
