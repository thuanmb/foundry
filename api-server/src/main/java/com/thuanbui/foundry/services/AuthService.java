package com.thuanbui.foundry.services;

import com.thuanbui.foundry.common.model.entity.OtpVerification;
import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.OtpVerificationRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.model.dto.SendOtpResponse;
import com.thuanbui.foundry.model.dto.UserProfileResponse;
import com.thuanbui.foundry.model.dto.VerifyOtpResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String OTP_PURPOSE = "LOGIN";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private final SmsOtpService smsOtpService;
    private final RefreshTokenService refreshTokenService;
    private final AuthProperties authProperties;
    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final MeterRegistry meterRegistry;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(OtpVerificationRepository otpRepository,
            UserRepository userRepository,
            SmsOtpService smsOtpService,
            RefreshTokenService refreshTokenService,
            AuthProperties authProperties,
            FirebaseTokenVerifier firebaseTokenVerifier,
            MeterRegistry meterRegistry) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.smsOtpService = smsOtpService;
        this.refreshTokenService = refreshTokenService;
        this.authProperties = authProperties;
        this.firebaseTokenVerifier = firebaseTokenVerifier;
        this.meterRegistry = meterRegistry;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    @Transactional
    public SendOtpResponse sendOtp(String phoneNumber) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long recentCount = otpRepository.countRecentByPhone(phoneNumber, oneHourAgo);
        if (recentCount >= authProperties.otp().rateLimitPerHour()) {
            throw new RateLimitExceededException();
        }

        Optional<OtpVerification> latestOtp = otpRepository.findLatestByPhoneAndNotExpired(phoneNumber);
        if (latestOtp.isPresent()) {
            Instant minSendTime = latestOtp.get().getCreatedAt()
                    .plus(authProperties.otp().cooldownSec(), ChronoUnit.SECONDS);
            if (Instant.now().isBefore(minSendTime)) {
                throw new CooldownNotMetException();
            }
        }

        String otp = generateOtp(authProperties.otp().length());
        Instant expiresAt = Instant.now().plus(authProperties.otp().expiryMinutes(), ChronoUnit.MINUTES);

        otpRepository.expirePreviousOtps(phoneNumber);

        OtpVerification verification = new OtpVerification();
        verification.setPhoneNumber(phoneNumber);
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setPurpose(OTP_PURPOSE);
        verification.setMaxAttempts(authProperties.otp().maxAttempts());
        verification.setExpiresAt(expiresAt);
        otpRepository.save(verification);

        // Response does not depend on SMS delivery to prevent phone-number enumeration
        smsOtpService.sendOtp(phoneNumber, otp);

        return new SendOtpResponse(true, authProperties.otp().expiryMinutes() * 60);
    }

    @Transactional(noRollbackFor = { OtpInvalidException.class, OtpMaxAttemptsException.class,
            OtpExpiredException.class })
    public VerifyOtpResponse verifyOtp(String phoneNumber, String otp) {
        OtpVerification verification = otpRepository.findLatestActiveByPhone(phoneNumber)
                .orElseThrow(OtpExpiredException::new);

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

        return loginOrRegister(phoneNumber);
    }

    @Transactional
    public VerifyOtpResponse verifyFirebaseToken(String idToken) {
        String phoneNumber = firebaseTokenVerifier.verifyAndGetPhoneNumber(idToken);
        return loginOrRegister(phoneNumber);
    }

    private VerifyOtpResponse loginOrRegister(String phoneNumber) {
        Optional<User> existingUser = userRepository.findByPhoneNumber(phoneNumber);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.isDeleted()) {
                throw new AccountDeletedException();
            }
            return buildResponse(user, false);
        }

        User user = new User();
        user.setPhoneNumber(phoneNumber);
        return buildResponse(userRepository.save(user), true);
    }

    private VerifyOtpResponse buildResponse(User user, boolean isNewUser) {
        meterRegistry.counter("auth_logins_total", "type", isNewUser ? "new" : "existing").increment();

        RefreshTokenService.TokenPair tokens = refreshTokenService.issueNewPair(user.getId());

        return new VerifyOtpResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessExpiresAt().toString(),
                tokens.refreshExpiresAt().toString(),
                UserProfileResponse.from(user),
                isNewUser);
    }

    private String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException() {
            super("Rate limit exceeded");
        }
    }

    public static class CooldownNotMetException extends RuntimeException {
        public CooldownNotMetException() {
            super("Cooldown period not met");
        }
    }

    public static class OtpExpiredException extends RuntimeException {
        public OtpExpiredException() {
            super("OTP expired");
        }
    }

    public static class OtpMaxAttemptsException extends RuntimeException {
        public OtpMaxAttemptsException() {
            super("Max OTP attempts exceeded");
        }
    }

    public static class OtpInvalidException extends RuntimeException {
        public OtpInvalidException() {
            super("Invalid OTP");
        }
    }

    public static class AccountDeletedException extends RuntimeException {
        public AccountDeletedException() {
            super("Account has been deleted");
        }
    }
}
