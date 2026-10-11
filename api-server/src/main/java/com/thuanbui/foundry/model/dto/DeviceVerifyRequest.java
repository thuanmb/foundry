package com.thuanbui.foundry.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceVerifyRequest(
        @NotBlank String attestationToken,
        @NotBlank String platform) {
}
