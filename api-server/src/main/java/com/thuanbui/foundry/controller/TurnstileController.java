package com.thuanbui.foundry.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import com.thuanbui.foundry.model.dto.TurnstileVerifyRequest;
import com.thuanbui.foundry.model.dto.TurnstileVerifyResponse;
import com.thuanbui.foundry.security.FeatureFlag;
import com.thuanbui.foundry.services.JweTokenService;
import com.thuanbui.foundry.services.TurnstileFailedException;
import com.thuanbui.foundry.services.TurnstileVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exchanges a Cloudflare Turnstile token for an anonymous JWE (web clients). */
@RestController
@RequestMapping("/api/v1/turnstile")
public class TurnstileController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(TurnstileController.class);

    private final TurnstileVerificationService turnstileService;
    private final JweTokenService jweTokenService;

    public TurnstileController(TurnstileVerificationService turnstileService, JweTokenService jweTokenService) {
        this.turnstileService = turnstileService;
        this.jweTokenService = jweTokenService;
    }

    @PostMapping("/verify")
    @FeatureFlag("anonymousWeb")
    public ResponseEntity<TurnstileVerifyResponse> verify(
            @Valid @RequestBody TurnstileVerifyRequest request,
            HttpServletRequest httpRequest) {
        if (!turnstileService.verify(request.token(), extractClientIp(httpRequest))) {
            throw new TurnstileFailedException();
        }

        String jweToken = jweTokenService.generateAnonymousToken("WEB", UuidCreator.getTimeOrderedEpoch());
        Instant expiresAt = jweTokenService.getAnonymousTokenExpiresAt();

        log.info("Anonymous token issued for WEB client: hash={}", jweTokenService.hashToken(jweToken));
        return ResponseEntity.ok(new TurnstileVerifyResponse(jweToken, expiresAt.toString()));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
