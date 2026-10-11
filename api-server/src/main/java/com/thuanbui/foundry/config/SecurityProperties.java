package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "security")
@Validated
public record SecurityProperties(
    @Valid RateLimit rateLimit,
    @Valid Request request,
    @Valid Cors cors
) {
    public record RateLimit(int publicEndpointsPerSec, int authEndpointsPerMin) {}
    public record Request(int maxBodySizeMb) {}
    public record Cors(String allowedOrigins, int maxAgeSec) {}
}
