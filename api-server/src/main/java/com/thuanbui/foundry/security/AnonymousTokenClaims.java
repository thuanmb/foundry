package com.thuanbui.foundry.security;

import java.time.Instant;
import java.util.UUID;

public record AnonymousTokenClaims(
        UUID anonymousId,
        String platform,
        Instant expiresAt) {
}
