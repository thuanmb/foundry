package com.thuanbui.foundry.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String otp) {
}
