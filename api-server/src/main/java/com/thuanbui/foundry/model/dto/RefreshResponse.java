package com.thuanbui.foundry.model.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        String accessExpiresAt,
        String refreshExpiresAt) {
}
