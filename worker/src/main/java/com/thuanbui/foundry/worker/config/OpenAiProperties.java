package com.thuanbui.foundry.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "openai")
@Validated
public record OpenAiProperties(
    @NotBlank String baseUrl,
    String apiKey
) {}
