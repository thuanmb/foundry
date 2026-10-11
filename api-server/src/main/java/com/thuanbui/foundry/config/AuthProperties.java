package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@ConfigurationProperties(prefix = "auth")
@Validated
public record AuthProperties(
        @Valid AccessToken accessToken,
        @Valid RefreshToken refreshToken,
        @Valid Otp otp,
        @Valid Jwe jwe) {
    public record AccessToken(@Min(1) int ttlMinutes, String issuer) {
    }

    public record RefreshToken(@Min(1) int ttlDays, @Min(1) int maxPerUser) {
    }

    public record Otp(@Min(4) int length, @Min(1) int expiryMinutes, @Min(1) int maxAttempts,
            @Min(1) int rateLimitPerHour, @Min(0) int cooldownSec) {
    }

    public record Jwe(@Min(1) int ttlHours, String algorithm, String secretKey) {
    }
}
