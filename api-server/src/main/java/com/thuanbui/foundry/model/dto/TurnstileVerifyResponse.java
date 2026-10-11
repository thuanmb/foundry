package com.thuanbui.foundry.model.dto;

public record TurnstileVerifyResponse(
        String jweToken,
        String expiresAt) {
}
