package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "cache")
@Validated
public record CacheProperties(@Valid Caffeine caffeine) {
    public record Caffeine(int featuresTtlSec, int mobileConfigTtlSec, int maxEntries) {
    }
}
