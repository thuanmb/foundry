package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "worker")
@Validated
public record WorkerProperties(
        @Valid Job job,
        @Valid Listen listen) {
    public record Job(int maxRetries, int heartbeatIntervalSec, int staleTimeoutMinutes, int staleCheckIntervalSec,
            int gracefulShutdownThresholdSec) {
    }

    public record Listen(String channel, String completeChannel, int fallbackPollSec) {
    }
}
