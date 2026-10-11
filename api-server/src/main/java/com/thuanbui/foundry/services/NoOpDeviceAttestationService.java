package com.thuanbui.foundry.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoOpDeviceAttestationService implements DeviceAttestationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpDeviceAttestationService.class);

    @Override
    public boolean verify(String attestationToken, String platform) {
        log.info("Device attestation verification skipped (no-op): platform={}", platform);
        return true;
    }
}
