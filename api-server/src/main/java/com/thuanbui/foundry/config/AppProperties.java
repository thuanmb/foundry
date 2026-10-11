package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
    @Valid Mobile mobile
) {
    public record Mobile(String minVersion, String latestVersion) {}
}
