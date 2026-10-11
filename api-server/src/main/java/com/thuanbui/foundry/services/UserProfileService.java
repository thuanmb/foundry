package com.thuanbui.foundry.services;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountProperties accountProperties;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository,
            OtpVerificationRepository otpRepository,
            RefreshTokenRepository refreshTokenRepository,
            AccountProperties accountProperties) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountProperties = accountProperties;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.isDeleted()) {
            throw new AccountDeletedException();
        }

        String displayName = request.displayName() == null ? null : request.displayName().trim();
        user.setDisplayName(displayName == null || displayName.isEmpty() ? null : displayName);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return UserProfileResponse.from(user);
    }

    @Transactional
    public DeleteAccountResponse deleteAccount(UUID userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.isDeleted()) {
            throw new AccountDeletedException();
        }

        OtpVerification verification = otpRepository.findLatestActiveByPhone(user.getPhoneNumber())
                .orElseThrow(OtpRequiredException::new);

        if (verification.getAttempts() >= verification.getMaxAttempts()) {
            throw new OtpMaxAttemptsException();
        }

        if (!passwordEncoder.matches(otp, verification.getOtpHash())) {
            verification.setAttempts(verification.getAttempts() + 1);
            otpRepository.save(verification);
            throw new OtpInvalidException();
        }

        verification.setVerified(true);
        otpRepository.save(verification);

        int graceDays = accountProperties.deletion().gracePeriodDays();
        Instant deletionScheduledAt = Instant.now().plus(graceDays, ChronoUnit.DAYS);

        user.setActive(false);
        user.setDeleted(true);
        user.setDeletionScheduledAt(deletionScheduledAt);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(userId);

        log.info("Account deletion scheduled for user {} at {}", userId, deletionScheduledAt);

        return new DeleteAccountResponse(true, deletionScheduledAt.toString());
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID userId) {
            super("User not found: " + userId);
        }
    }

    public static class AccountDeletedException extends RuntimeException {
        public AccountDeletedException() {
            super("Account has been deleted");
        }
    }

    public static class OtpRequiredException extends RuntimeException {
        public OtpRequiredException() {
            super("OTP required for account deletion");
        }
    }
}
