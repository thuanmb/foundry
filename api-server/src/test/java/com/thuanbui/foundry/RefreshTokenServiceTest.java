package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.common.model.entity.RefreshToken;
import com.thuanbui.foundry.common.repository.RefreshTokenRepository;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.services.JwtTokenService;
import com.thuanbui.foundry.services.RefreshTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private AuthProperties authProperties;

    private RefreshTokenService refreshTokenService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        AuthProperties.RefreshToken refreshConfig = new AuthProperties.RefreshToken(30, 5);
        when(authProperties.refreshToken()).thenReturn(refreshConfig);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenService, authProperties);
        userId = UUID.randomUUID();
    }

    @Test
    void issueNewPair_shouldCreateTokenPair() {
        when(jwtTokenService.generateAccessToken(userId)).thenReturn("access-token");
        when(jwtTokenService.getAccessExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.countByUserIdAndRevokedFalse(userId)).thenReturn(0L);

        RefreshTokenService.TokenPair pair = refreshTokenService.issueNewPair(userId);

        assertThat(pair.accessToken()).isEqualTo("access-token");
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.accessExpiresAt()).isNotNull();
        assertThat(pair.refreshExpiresAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getTokenHash()).isNotBlank();
        assertThat(captor.getValue().isRevoked()).isFalse();
    }

    @Test
    void issueNewPair_shouldEnforceMaxTokensPerUser() {
        when(jwtTokenService.generateAccessToken(userId)).thenReturn("access-token");
        when(jwtTokenService.getAccessExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.countByUserIdAndRevokedFalse(userId)).thenReturn(6L);

        refreshTokenService.issueNewPair(userId);

        verify(refreshTokenRepository).deleteOldestActiveByUserId(userId, 5);
    }

    @Test
    void rotateToken_shouldRotateSuccessfully() {
        String rawToken = "raw-refresh-token";
        String oldHash = sha256(rawToken);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(userId);
        stored.setTokenHash(oldHash);
        stored.setExpiresAt(Instant.now().plusSeconds(86400));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(stored));
        when(jwtTokenService.generateAccessToken(userId)).thenReturn("new-access-token");
        when(jwtTokenService.getAccessExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.countByUserIdAndRevokedFalse(userId)).thenReturn(0L);

        RefreshTokenService.TokenPair pair = refreshTokenService.rotateToken(rawToken);

        assertThat(pair.accessToken()).isEqualTo("new-access-token");
        assertThat(pair.refreshToken()).isNotBlank();
        assertThat(pair.refreshToken()).isNotEqualTo(rawToken);

        // Old token should be revoked
        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.getRevokedAt()).isNotNull();
    }

    @Test
    void rotateToken_shouldDetectTheft_whenTokenAlreadyRevoked() {
        String rawToken = "raw-refresh-token";
        String hash = sha256(rawToken);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(userId);
        stored.setTokenHash(hash);
        stored.setRevoked(true);
        stored.setExpiresAt(Instant.now().plusSeconds(86400));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThrows(RefreshTokenService.TokenTheftDetectedException.class,
                () -> refreshTokenService.rotateToken(rawToken));

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void rotateToken_shouldRejectExpiredToken() {
        String rawToken = "expired-raw-token";
        String hash = sha256(rawToken);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(userId);
        stored.setTokenHash(hash);
        stored.setRevoked(false);
        stored.setExpiresAt(Instant.now().minusSeconds(1));

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        assertThrows(RefreshTokenService.TokenExpiredException.class,
                () -> refreshTokenService.rotateToken(rawToken));
    }

    @Test
    void rotateToken_shouldRejectUnknownToken() {
        String hash = sha256("unknown");
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        assertThrows(RefreshTokenService.TokenInvalidException.class,
                () -> refreshTokenService.rotateToken("unknown"));
    }

    @Test
    void revokeToken_shouldRevokeAndSave() {
        String rawToken = "token-to-revoke";
        String hash = sha256(rawToken);

        RefreshToken stored = new RefreshToken();
        stored.setUserId(userId);
        stored.setTokenHash(hash);
        stored.setRevoked(false);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        refreshTokenService.revokeToken(rawToken);

        assertThat(stored.isRevoked()).isTrue();
        assertThat(stored.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revokeToken_shouldDoNothing_whenTokenNotFound() {
        String hash = sha256("nonexistent");
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        refreshTokenService.revokeToken("nonexistent");

        verify(refreshTokenRepository).findByTokenHash(hash);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
