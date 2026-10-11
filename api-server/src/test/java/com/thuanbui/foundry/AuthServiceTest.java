package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.common.model.entity.OtpVerification;
import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.OtpVerificationRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.model.dto.SendOtpResponse;
import com.thuanbui.foundry.model.dto.VerifyOtpResponse;
import com.thuanbui.foundry.services.AuthService;
import com.thuanbui.foundry.services.AuthService.CooldownNotMetException;
import com.thuanbui.foundry.services.AuthService.OtpExpiredException;
import com.thuanbui.foundry.services.AuthService.OtpInvalidException;
import com.thuanbui.foundry.services.AuthService.OtpMaxAttemptsException;
import com.thuanbui.foundry.services.AuthService.RateLimitExceededException;
import com.thuanbui.foundry.services.FirebaseTokenVerifier;
import com.thuanbui.foundry.services.RefreshTokenService;
import com.thuanbui.foundry.services.SmsOtpService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private OtpVerificationRepository otpRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SmsOtpService smsOtpService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuthProperties authProperties;
    @Mock
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Spy
    private SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private AuthService authService;

    private AuthProperties.Otp otpConfig;
    private AuthProperties.RefreshToken refreshTokenConfig;

    @BeforeEach
    void setUp() {
        otpConfig = new AuthProperties.Otp(6, 5, 5, 3, 60);
        refreshTokenConfig = new AuthProperties.RefreshToken(30, 5);
        AuthProperties.AccessToken accessTokenConfig = new AuthProperties.AccessToken(60, "foundry");
        when(authProperties.otp()).thenReturn(otpConfig);
        when(authProperties.refreshToken()).thenReturn(refreshTokenConfig);
        when(authProperties.accessToken()).thenReturn(accessTokenConfig);
    }

    @Test
    void sendOtp_shouldReturnSuccess_whenValidPhone() {
        when(otpRepository.countRecentByPhone(anyString(), any())).thenReturn(0L);
        when(otpRepository.findLatestByPhoneAndNotExpired(anyString())).thenReturn(Optional.empty());

        SendOtpResponse response = authService.sendOtp("+84912345678");

        assertTrue(response.otpSent());
        assertEquals(300, response.expiresInSec());
        verify(smsOtpService).sendOtp(eq("+84912345678"), anyString());
        verify(otpRepository).save(any(OtpVerification.class));
    }

    @Test
    void sendOtp_shouldThrowRateLimited_whenTooManyRequests() {
        when(otpRepository.countRecentByPhone(anyString(), any())).thenReturn(3L);

        assertThrows(RateLimitExceededException.class,
                () -> authService.sendOtp("+84912345678"));
    }

    @Test
    void sendOtp_shouldThrowCooldown_whenWithinCooldownPeriod() {
        when(otpRepository.countRecentByPhone(anyString(), any())).thenReturn(0L);
        OtpVerification recentOtp = new OtpVerification();
        recentOtp.setCreatedAt(Instant.now().minusSeconds(10));
        when(otpRepository.findLatestByPhoneAndNotExpired("+84912345678"))
                .thenReturn(Optional.of(recentOtp));

        assertThrows(CooldownNotMetException.class,
                () -> authService.sendOtp("+84912345678"));
    }

    @Test
    void verifyOtp_shouldThrowExpired_whenNoActiveOtp() {
        when(otpRepository.findLatestActiveByPhone("+84912345678")).thenReturn(Optional.empty());

        assertThrows(OtpExpiredException.class,
                () -> authService.verifyOtp("+84912345678", "123456"));
    }

    @Test
    void verifyOtp_shouldThrowMaxAttempts_whenExceeded() {
        OtpVerification verification = new OtpVerification();
        verification.setAttempts(5);
        verification.setMaxAttempts(5);
        verification.setOtpHash("hash");
        when(otpRepository.findLatestActiveByPhone("+84912345678"))
                .thenReturn(Optional.of(verification));

        assertThrows(OtpMaxAttemptsException.class,
                () -> authService.verifyOtp("+84912345678", "123456"));
    }

    @Test
    void verifyOtp_shouldThrowInvalid_whenOtpMismatch() {
        OtpVerification verification = new OtpVerification();
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setOtpHash(new BCryptPasswordEncoder(10).encode("999999"));
        when(otpRepository.findLatestActiveByPhone("+84912345678"))
                .thenReturn(Optional.of(verification));

        assertThrows(OtpInvalidException.class,
                () -> authService.verifyOtp("+84912345678", "123456"));
        assertEquals(1, verification.getAttempts());
    }

    @Test
    void verifyOtp_shouldCreateNewUser_whenPhoneNotRegistered() {
        String phone = "+84912345678";
        String otp = "123456";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        OtpVerification verification = new OtpVerification();
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setOtpHash(encoder.encode(otp));

        when(otpRepository.findLatestActiveByPhone(phone)).thenReturn(Optional.of(verification));
        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(refreshTokenService.issueNewPair(any(UUID.class)))
                .thenReturn(new RefreshTokenService.TokenPair("access", "refresh",
                        Instant.now().plusSeconds(3600), Instant.now().plusSeconds(86400)));

        VerifyOtpResponse response = authService.verifyOtp(phone, otp);

        assertTrue(response.isNewUser());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotNull(response.user());
        assertEquals(phone, response.user().phoneNumber());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyOtp_shouldReturnExistingUser_whenPhoneRegistered() {
        String phone = "+84912345678";
        String otp = "654321";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        OtpVerification verification = new OtpVerification();
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setOtpHash(encoder.encode(otp));

        User existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setPhoneNumber(phone);
        existingUser.setDisplayName("Existing");

        when(otpRepository.findLatestActiveByPhone(phone)).thenReturn(Optional.of(verification));
        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(existingUser));
        when(refreshTokenService.issueNewPair(existingUser.getId()))
                .thenReturn(new RefreshTokenService.TokenPair("access", "refresh",
                        Instant.now().plusSeconds(3600), Instant.now().plusSeconds(86400)));

        VerifyOtpResponse response = authService.verifyOtp(phone, otp);

        assertFalse(response.isNewUser());
        assertEquals("Existing", response.user().displayName());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyOtp_shouldRejectDeletedAccount() {
        String phone = "+84912345678";
        String otp = "111111";

        OtpVerification verification = new OtpVerification();
        verification.setAttempts(0);
        verification.setMaxAttempts(5);
        verification.setOtpHash(new BCryptPasswordEncoder(10).encode(otp));

        User deletedUser = new User();
        deletedUser.setId(UUID.randomUUID());
        deletedUser.setPhoneNumber(phone);
        deletedUser.setDeleted(true);

        when(otpRepository.findLatestActiveByPhone(phone)).thenReturn(Optional.of(verification));
        when(userRepository.findByPhoneNumber(phone)).thenReturn(Optional.of(deletedUser));

        assertThrows(AuthService.AccountDeletedException.class, () -> authService.verifyOtp(phone, otp));
        verify(refreshTokenService, never()).issueNewPair(any());
    }
}
