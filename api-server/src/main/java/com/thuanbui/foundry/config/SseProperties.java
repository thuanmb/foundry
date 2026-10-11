package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sse")
@Validated
public record SseProperties(
    int heartbeatIntervalSec,
    int connectionTimeoutSec
) {}
