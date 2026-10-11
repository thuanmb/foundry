package com.thuanbui.foundry.security;

import java.util.UUID;

public record TokenClaims(
        UUID userId,
        TokenType tokenType) {

    public enum TokenType {
        JWT,
        JWE
    }
}
