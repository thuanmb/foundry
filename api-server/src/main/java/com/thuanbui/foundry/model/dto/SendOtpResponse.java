package com.thuanbui.foundry.model.dto;

public record SendOtpResponse(
        boolean otpSent,
        int expiresInSec) {
}
