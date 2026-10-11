package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thuanbui.foundry.common.model.entity.OtpVerification;
import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.OtpVerificationRepository;
import com.thuanbui.foundry.common.repository.RefreshTokenRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.config.AccountProperties;
import com.thuanbui.foundry.model.dto.DeleteAccountResponse;
import com.thuanbui.foundry.model.dto.UpdateProfileRequest;
import com.thuanbui.foundry.model.dto.UserProfileResponse;
import com.thuanbui.foundry.services.AuthService.OtpInvalidException;
import com.thuanbui.foundry.services.AuthService.OtpMaxAttemptsException;
import com.thuanbui.foundry.services.UserProfileService;
import com.thuanbui.foundry.services.UserProfileService.AccountDeletedException;
import com.thuanbui.foundry.services.UserProfileService.OtpRequiredException;
import com.thuanbui.foundry.services.UserProfileService.UserNotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpVerificationRepository otpRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AccountProperties accountProperties;

    @InjectMocks
    private UserProfileService userProfileService;

    private User user;
    private UUID userId;
    private AccountProperties.Deletion deletionConfig;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setPhoneNumber("+84912345678");
        user.setDisplayName("Alice");
        user.setCreatedAt(Instant.now());

        deletionConfig = new AccountProperties.Deletion(30, true);
        when(accountProperties.deletion()).thenReturn(deletionConfig);
    }

    @Test
    void getProfile_shouldReturnUserProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getProfile(userId);

        assertNotNull(response);
        assertEquals(userId.toString(), response.id());
        assertEquals("+84912345678", response.phoneNumber());
        assertEquals("Alice", response.displayName());
    }

    @Test
    void getProfile_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userProfileService.getProfile(userId));
    }

    @Test
    void updateProfile_shouldUpdateAndReturnProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest("  Bob  ");
        UserProfileResponse response = userProfileService.updateProfile(userId, request);

        assertEquals("Bob", response.displayName());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_blankDisplayNameShouldClearIt() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.updateProfile(userId, new UpdateProfileRequest("   "));

        assertNull(response.displayName());
    }

    @Test
    void updateProfile_shouldThrowWhenAccountDeleted() {
        user.setDeleted(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateProfileRequest request = new UpdateProfileRequest("Bob");
        assertThrows(AccountDeletedException.class, () -> userProfileService.updateProfile(userId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccount_shouldScheduleDeletionAndRevokeTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OtpVerification verification = new OtpVerification();
        verification.setPhoneNumber("+84912345678");
        verification.setOtpHash(new BCryptPasswordEncoder(10).encode("123456"));
        verification.setMaxAttempts(5);
        verification.setAttempts(0);
        when(otpRepository.findLatestActiveByPhone("+84912345678")).thenReturn(Optional.of(verification));

        DeleteAccountResponse response = userProfileService.deleteAccount(userId, "123456");

        assertTrue(response.success());
        assertNotNull(response.deletionScheduledAt());
        assertTrue(user.isDeleted());
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void deleteAccount_shouldThrowWhenOtpNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(otpRepository.findLatestActiveByPhone("+84912345678")).thenReturn(Optional.empty());

        assertThrows(OtpRequiredException.class, () -> userProfileService.deleteAccount(userId, "123456"));
    }

    @Test
    void deleteAccount_shouldThrowWhenOtpInvalid() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OtpVerification verification = new OtpVerification();
        verification.setPhoneNumber("+84912345678");
        verification.setOtpHash(new BCryptPasswordEncoder(10).encode("999999"));
        verification.setMaxAttempts(5);
        verification.setAttempts(0);
        when(otpRepository.findLatestActiveByPhone("+84912345678")).thenReturn(Optional.of(verification));

        assertThrows(OtpInvalidException.class, () -> userProfileService.deleteAccount(userId, "123456"));
    }

    @Test
    void deleteAccount_shouldThrowWhenMaxAttemptsExceeded() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        OtpVerification verification = new OtpVerification();
        verification.setPhoneNumber("+84912345678");
        verification.setOtpHash(new BCryptPasswordEncoder(10).encode("123456"));
        verification.setMaxAttempts(5);
        verification.setAttempts(5);
        when(otpRepository.findLatestActiveByPhone("+84912345678")).thenReturn(Optional.of(verification));

        assertThrows(OtpMaxAttemptsException.class, () -> userProfileService.deleteAccount(userId, "123456"));
    }

    @Test
    void deleteAccount_shouldThrowWhenAccountAlreadyDeleted() {
        user.setDeleted(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(AccountDeletedException.class, () -> userProfileService.deleteAccount(userId, "123456"));
    }
}
