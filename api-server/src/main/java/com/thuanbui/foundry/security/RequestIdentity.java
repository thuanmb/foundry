package com.thuanbui.foundry.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

/** Reads the caller identity populated by {@link TokenAuthenticationFilter}. */
public final class RequestIdentity {

    public static final String TOKEN_CLAIMS_ATTR = "tokenClaims";
    public static final String ANONYMOUS_TOKEN_HASH_ATTR = "anonymousTokenHash";

    private RequestIdentity() {
    }

    /** Returns the authenticated (JWT) user id, or null for anonymous / unauthenticated callers. */
    public static UUID userId(HttpServletRequest request) {
        if (request.getAttribute(TOKEN_CLAIMS_ATTR) instanceof TokenClaims claims
                && claims.tokenType() == TokenClaims.TokenType.JWT) {
            return claims.userId();
        }
        return null;
    }

    /** Returns {@code user:<uuid>} for JWT, {@code anon:<token-hash>} for JWE, or null when unauthenticated. */
    public static String ownerKey(HttpServletRequest request) {
        UUID userId = userId(request);
        if (userId != null) {
            return "user:" + userId;
        }
        if (request.getAttribute(ANONYMOUS_TOKEN_HASH_ATTR) instanceof String tokenHash && !tokenHash.isBlank()) {
            return "anon:" + tokenHash;
        }
        return null;
    }
}
