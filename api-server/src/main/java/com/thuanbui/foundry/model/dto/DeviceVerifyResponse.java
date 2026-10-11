package com.thuanbui.foundry.model.dto;

public record DeviceVerifyResponse(
        String jweToken,
        String expiresAt) {
}
