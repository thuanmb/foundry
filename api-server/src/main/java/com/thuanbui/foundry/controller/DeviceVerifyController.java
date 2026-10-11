package com.thuanbui.foundry.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import com.thuanbui.foundry.model.dto.DeviceVerifyRequest;
import com.thuanbui.foundry.model.dto.DeviceVerifyResponse;
import com.thuanbui.foundry.services.AttestationFailedException;
import com.thuanbui.foundry.services.DeviceAttestationService;
import com.thuanbui.foundry.services.JweTokenService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges an App Attest / Play Integrity token for an anonymous JWE (mobile clients). */
@RestController
@RequestMapping("/api/v1/device")
public class DeviceVerifyController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(DeviceVerifyController.class);

    private final DeviceAttestationService attestationService;
    private final JweTokenService jweTokenService;

    public DeviceVerifyController(DeviceAttestationService attestationService, JweTokenService jweTokenService) {
        this.attestationService = attestationService;
        this.jweTokenService = jweTokenService;
    }

    @PostMapping("/verify")
    public ResponseEntity<DeviceVerifyResponse> verify(@Valid @RequestBody DeviceVerifyRequest request) {
        if (!attestationService.verify(request.attestationToken(), request.platform())) {
            throw new AttestationFailedException();
        }

        String jweToken = jweTokenService.generateAnonymousToken("MOBILE", UuidCreator.getTimeOrderedEpoch());
        Instant expiresAt = jweTokenService.getAnonymousTokenExpiresAt();

        log.info("Anonymous token issued for MOBILE client via device attestation: hash={}",
                jweTokenService.hashToken(jweToken));
        return ResponseEntity.ok(new DeviceVerifyResponse(jweToken, expiresAt.toString()));
    }
}
