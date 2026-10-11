package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@ConfigurationProperties(prefix = "polling")
@Validated
public record PollingProperties(
        @Min(1) int initialDelaySec,
        @Min(1) int maxDelaySec,
        double backoffMultiplier) {
}
