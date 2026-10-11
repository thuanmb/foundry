package com.thuanbui.foundry.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "worker")
@Validated
public record WorkerProperties(
        @Valid Job job,
        @Valid Listen listen,
        @Valid Llm llm,
        @Valid @DefaultValue Echo echo) {
    public record Job(int maxRetries, int heartbeatIntervalSec, int staleTimeoutMinutes, int staleCheckIntervalSec,
            int gracefulShutdownThresholdSec) {
    }

    // Channel names are interpolated into LISTEN statements, so restrict them to
    // safe identifiers
    public record Listen(
            @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]*") String channel,
            @NotBlank @Pattern(regexp = "[a-z_][a-z0-9_]*") String completeChannel,
            int fallbackPollSec) {
    }

    public record Llm(String model, int timeoutSec, int maxTokens, double temperature) {
    }

    public record Echo(
            @DefaultValue("false") boolean llmEnabled,
            @DefaultValue("30") @Min(0) int maxDelaySec) {
    }
}
