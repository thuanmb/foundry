package com.thuanbui.foundry.model.dto;

public record VerifyOtpResponse(
        String accessToken,
        String refreshToken,
        String accessExpiresAt,
        String refreshExpiresAt,
        UserProfileResponse user,
        boolean isNewUser) {
}
