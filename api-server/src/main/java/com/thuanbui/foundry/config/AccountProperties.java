package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "account")
@Validated
public record AccountProperties(
    @Valid Deletion deletion,
    @Valid Retention retention
) {
    public record Deletion(int gracePeriodDays, boolean anonymizeOnDelete) {}
    public record Retention(int inactiveDays) {}
}
