package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.security.AnonymousTokenClaims;
import com.thuanbui.foundry.services.JweTokenService;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JweTokenServiceTest {

    private JweTokenService jweTokenService;
    private String validSecretKey;

    @BeforeEach
    void setUp() {
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = (byte) i;
        }
        validSecretKey = Base64.getEncoder().encodeToString(keyBytes);
        jweTokenService = buildService(24);
    }

    private JweTokenService buildService(int ttlHours) {
        AuthProperties.Jwe jwe = new AuthProperties.Jwe(ttlHours, "A256GCM", validSecretKey);
        AuthProperties authProperties = new AuthProperties(
                new AuthProperties.AccessToken(60, "test"),
                new AuthProperties.RefreshToken(30, 5),
                new AuthProperties.Otp(6, 5, 5, 3, 60),
                jwe);
        return new JweTokenService(authProperties);
    }

    @Test
    void generateAndValidate_roundTrip_shouldSucceed() {
        UUID anonymousId = UUID.randomUUID();
        String token = jweTokenService.generateAnonymousToken("WEB", anonymousId);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 5, "JWE should have 5 parts");

        AnonymousTokenClaims claims = jweTokenService.validateToken(token);
        assertEquals(anonymousId, claims.anonymousId());
        assertEquals("WEB", claims.platform());
        assertNotNull(claims.expiresAt());
    }

    @Test
    void validate_expiredToken_shouldThrowException() {
        JweTokenService expiredService = buildService(-1);
        String token = expiredService.generateAnonymousToken("WEB", UUID.randomUUID());

        assertThrows(JweTokenService.TokenExpiredException.class, () -> {
            expiredService.validateToken(token);
        });
    }

    @Test
    void validate_invalidToken_shouldThrowException() {
        assertThrows(JweTokenService.TokenInvalidException.class, () -> {
            jweTokenService.validateToken("invalid.token.string");
        });
    }

    @Test
    void validate_tamperedToken_shouldThrowException() {
        String token = jweTokenService.generateAnonymousToken("WEB", UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(JweTokenService.TokenInvalidException.class, () -> {
            jweTokenService.validateToken(tampered);
        });
    }

    @Test
    void hashToken_shouldReturnConsistentHash() {
        String token = jweTokenService.generateAnonymousToken("WEB", UUID.randomUUID());
        String hash1 = jweTokenService.hashToken(token);
        String hash2 = jweTokenService.hashToken(token);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void generateAnonymousToken_shouldIncludePlatformClaim() {
        String token = jweTokenService.generateAnonymousToken("MOBILE", UUID.randomUUID());
        AnonymousTokenClaims claims = jweTokenService.validateToken(token);
        assertEquals("MOBILE", claims.platform());
    }

    @Test
    void constructor_nullSecretKey_shouldThrowException() {
        AuthProperties.Jwe jwe = new AuthProperties.Jwe(24, "A256GCM", null);
        AuthProperties authProperties = new AuthProperties(
                new AuthProperties.AccessToken(60, "test"),
                new AuthProperties.RefreshToken(30, 5),
                new AuthProperties.Otp(6, 5, 5, 3, 60),
                jwe);

        assertThrows(IllegalStateException.class, () -> new JweTokenService(authProperties));
    }

    @Test
    void constructor_blankSecretKey_shouldThrowException() {
        AuthProperties.Jwe jwe = new AuthProperties.Jwe(24, "A256GCM", "   ");
        AuthProperties authProperties = new AuthProperties(
                new AuthProperties.AccessToken(60, "test"),
                new AuthProperties.RefreshToken(30, 5),
                new AuthProperties.Otp(6, 5, 5, 3, 60),
                jwe);

        assertThrows(IllegalStateException.class, () -> new JweTokenService(authProperties));
    }

    @Test
    void constructor_shortSecretKey_shouldThrowException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        AuthProperties.Jwe jwe = new AuthProperties.Jwe(24, "A256GCM", shortKey);
        AuthProperties authProperties = new AuthProperties(
                new AuthProperties.AccessToken(60, "test"),
                new AuthProperties.RefreshToken(30, 5),
                new AuthProperties.Otp(6, 5, 5, 3, 60),
                jwe);

        assertThrows(IllegalStateException.class, () -> new JweTokenService(authProperties));
    }

    @Test
    void getAnonymousTokenExpiresAt_shouldReturnFutureInstant() {
        Instant expiresAt = jweTokenService.getAnonymousTokenExpiresAt();
        assertTrue(expiresAt.isAfter(Instant.now()));
    }
}
