package com.thuanbui.foundry.model.dto;

import jakarta.validation.constraints.NotBlank;

public record TurnstileVerifyRequest(
        @NotBlank(message = "{validation.turnstile.token.required}") String token) {
}
