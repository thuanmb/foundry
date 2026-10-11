package com.thuanbui.foundry.services;

import com.thuanbui.foundry.common.model.entity.RefreshToken;
import com.thuanbui.foundry.common.repository.RefreshTokenRepository;
import com.thuanbui.foundry.config.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
            JwtTokenService jwtTokenService,
            AuthProperties authProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
    }

    @Transactional
    public TokenPair issueNewPair(UUID userId) {
        String accessToken = jwtTokenService.generateAccessToken(userId);
        String rawRefreshToken = generateSecureToken();
        String tokenHash = sha256(rawRefreshToken);

        Instant refreshExpiry = Instant.now()
                .plus(authProperties.refreshToken().ttlDays(), ChronoUnit.DAYS);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(tokenHash);
        rt.setDeviceInfo(null);
        rt.setExpiresAt(refreshExpiry);
        refreshTokenRepository.save(rt);

        enforceMaxTokensPerUser(userId);

        return new TokenPair(accessToken, rawRefreshToken,
                jwtTokenService.getAccessExpiresAt(), refreshExpiry);
    }

    @Transactional(noRollbackFor = TokenTheftDetectedException.class)
    public TokenPair rotateToken(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenInvalidException("Refresh token not found"));

        if (stored.isRevoked()) {
            log.warn("Theft detected: reused revoked refresh token for user {}", stored.getUserId());
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw new TokenTheftDetectedException();
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Refresh token expired");
        }

        stored.setRevoked(true);
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueNewPair(stored.getUserId());
    }

    @Transactional
    public void revokeToken(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public Instant getRefreshExpiresAt() {
        return Instant.now().plus(authProperties.refreshToken().ttlDays(), ChronoUnit.DAYS);
    }

    private void enforceMaxTokensPerUser(UUID userId) {
        long count = refreshTokenRepository.countByUserIdAndRevokedFalse(userId);
        int maxTokens = authProperties.refreshToken().maxPerUser();
        if (count > maxTokens) {
            int toDelete = (int) (count - maxTokens);
            refreshTokenRepository.deleteOldestActiveByUserId(userId, maxTokens);
            log.info("Enforced max tokens for user {}: removed {} oldest", userId, toDelete);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {
    }

    public static class TokenInvalidException extends RuntimeException {
        public TokenInvalidException(String message) {
            super(message);
        }
    }

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

    public static class TokenTheftDetectedException extends RuntimeException {
        public TokenTheftDetectedException() {
            super("Token theft detected — all sessions revoked");
        }
    }
}
