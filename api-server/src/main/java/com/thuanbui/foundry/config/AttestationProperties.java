package com.thuanbui.foundry.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "attestation")
@Validated
public record AttestationProperties(
        @NotBlank String iosAppId,
        String iosEnvironment,
        @NotBlank String androidPackageName,
        String androidDecryptionKey,
        String androidVerificationKey,
        boolean bypassEnabled) {
}
