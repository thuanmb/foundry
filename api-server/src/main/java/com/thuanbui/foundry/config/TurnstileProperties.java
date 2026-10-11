package com.thuanbui.foundry.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "turnstile")
@Validated
public record TurnstileProperties(
        @NotBlank String secretKey,
        @NotBlank String verifyUrl,
        @Min(1) int timeoutSec) {
}
