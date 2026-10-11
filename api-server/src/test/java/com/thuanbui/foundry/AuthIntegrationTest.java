package com.thuanbui.foundry;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.common.model.entity.OtpVerification;
import com.thuanbui.foundry.common.model.entity.RefreshToken;
import com.thuanbui.foundry.common.model.entity.User;
import com.thuanbui.foundry.common.repository.OtpVerificationRepository;
import com.thuanbui.foundry.common.repository.RefreshTokenRepository;
import com.thuanbui.foundry.common.repository.UserRepository;
import com.thuanbui.foundry.model.dto.RefreshRequest;
import com.thuanbui.foundry.model.dto.RefreshResponse;
import com.thuanbui.foundry.model.dto.LogoutRequest;
import com.thuanbui.foundry.model.dto.LogoutResponse;
import com.thuanbui.foundry.model.dto.SendOtpRequest;
import com.thuanbui.foundry.model.dto.SendOtpResponse;
import com.thuanbui.foundry.model.dto.VerifyOtpRequest;
import com.thuanbui.foundry.model.dto.VerifyOtpResponse;
import com.thuanbui.foundry.services.SmsOtpService;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.clean-disabled", () -> "false");
        // Override SMS config — test config provides TestSmsOtpService
        registry.add("sms.provider", () -> "test");
        // Dummy JWT secret for token signing in tests
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < 32; i++)
            keyBytes[i] = (byte) i;
        String jwtSecret = Base64.getEncoder().encodeToString(keyBytes);
        registry.add("auth.access-token.secret-key", () -> jwtSecret);
    }

    @TestConfiguration
    static class TestSmsConfig {
        @Bean
        @Primary
        SmsOtpService testSmsOtpService() {
            return new TestSmsOtpService();
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpVerificationRepository otpRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private TestSmsOtpService testSmsOtpService;

    private static final String PHONE = "+84912345678";
    private static final String PHONE2 = "+84987654321";

    @BeforeEach
    void setUp() {
        // Delete in FK-safe order
        refreshTokenRepository.deleteAll();
        otpRepository.deleteAll();
        userRepository.deleteAll();
        testSmsOtpService.clear();
    }

    @AfterEach
    void tearDown() {
        testSmsOtpService.clear();
    }

    // ─── Send OTP tests ────────────────────────────────────────────────

    @Test
    void sendOtp_shouldReturnSuccess_whenValidPhone() throws Exception {
        SendOtpRequest req = new SendOtpRequest(PHONE);
        String json = objectMapper.writeValueAsString(req);

        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        SendOtpResponse response = objectMapper.readValue(responseJson, SendOtpResponse.class);
        assertThat(response.otpSent()).isTrue();
        assertThat(response.expiresInSec()).isEqualTo(300);

        // Verify OTP was stored (bcrypt-hashed)
        assertThat(otpRepository.findLatestByPhoneAndNotExpired(PHONE)).isPresent();

        // Verify SMS was sent via test service
        String capturedOtp = testSmsOtpService.getLastOtp(PHONE);
        assertThat(capturedOtp).isNotNull().hasSize(6);
    }

    @Test
    void sendOtp_shouldRejectInvalidPhoneFormat() throws Exception {
        String json = "{\"phoneNumber\":\"not-a-phone\"}";

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void sendOtp_shouldEnforceRateLimit() throws Exception {
        // Insert 3 OTP records from over an hour ago (past rate limit window)
        // and set them as expired so cooldown doesn't trigger
        for (int i = 0; i < 3; i++) {
            OtpVerification oldOtp = new OtpVerification();
            oldOtp.setPhoneNumber(PHONE);
            oldOtp.setOtpHash("$2a$10$" + "x".repeat(53));
            oldOtp.setPurpose("LOGIN");
            oldOtp.setMaxAttempts(5);
            oldOtp.setExpiresAt(Instant.now().minusSeconds(1));
            oldOtp.setCreatedAt(Instant.now().minusSeconds(120 + i));
            oldOtp.setVerified(true);
            otpRepository.save(oldOtp);
        }

        String json = objectMapper.writeValueAsString(new SendOtpRequest(PHONE));

        // This request should hit rate limit since 3 OTPs were sent in the last hour
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(429));
    }

    @Test
    void sendOtp_shouldEnforceCooldown() throws Exception {
        String json = objectMapper.writeValueAsString(new SendOtpRequest(PHONE));

        // First OTP
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // Second OTP immediately — should be blocked by cooldown
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(429));
    }

    @Test
    void sendOtp_shouldReturnIdenticalResponse_forNewAndExistingPhone() throws Exception {
        // Pre-register a user
        User user = new User();
        user.setPhoneNumber(PHONE2);
        userRepository.save(user);

        SendOtpRequest req = new SendOtpRequest(PHONE2);
        String json = objectMapper.writeValueAsString(req);

        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        SendOtpResponse response = objectMapper.readValue(responseJson, SendOtpResponse.class);
        // Should not leak whether phone exists
        assertThat(response.otpSent()).isTrue();
        assertThat(response.expiresInSec()).isEqualTo(300);
        // No other fields in response
    }

    // ─── Verify OTP tests ──────────────────────────────────────────────

    @Test
    void verifyOtp_shouldCreateNewUserAndIssueTokens() throws Exception {
        // Step 1: Send OTP
        sendOtp(PHONE);
        String otp = testSmsOtpService.getLastOtp(PHONE);

        // Step 2: Verify OTP
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        VerifyOtpResponse response = objectMapper.readValue(responseJson, VerifyOtpResponse.class);
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.accessExpiresAt()).isNotBlank();
        assertThat(response.refreshExpiresAt()).isNotBlank();
        assertThat(response.user().phoneNumber()).isEqualTo(PHONE);
        assertThat(response.user().id()).isNotBlank();

        // Verify user created in DB
        User dbUser = userRepository.findByPhoneNumber(PHONE).orElseThrow();

        // Verify refresh token stored
        long tokenCount = refreshTokenRepository.countByUserIdAndRevokedFalse(dbUser.getId());
        assertThat(tokenCount).isEqualTo(1);

        // Verify OTP marked as verified
        OtpVerification otpRecord = otpRepository.findAll().stream()
                .filter(o -> o.getPhoneNumber().equals(PHONE))
                .findFirst().orElseThrow();
        assertThat(otpRecord.isVerified()).isTrue();

        // Verify access token is a valid JWT (can decode)
        String accessToken = response.accessToken();
        String[] parts = accessToken.split("\\.");
        assertThat(parts).hasSize(3); // header.payload.signature
    }

    @Test
    void verifyOtp_shouldReturnExistingUser() throws Exception {
        // Pre-create user
        User user = new User();
        user.setPhoneNumber(PHONE);
        user.setDisplayName("Old User");
        userRepository.save(user);

        // Send & capture OTP
        sendOtp(PHONE);
        String otp = testSmsOtpService.getLastOtp(PHONE);

        // Verify
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        VerifyOtpResponse response = objectMapper.readValue(responseJson, VerifyOtpResponse.class);
        assertThat(response.isNewUser()).isFalse();
        assertThat(response.user().phoneNumber()).isEqualTo(PHONE);
        assertThat(response.user().displayName()).isEqualTo("Old User");

        // Should not create duplicate user
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void verifyOtp_shouldRejectInvalidOtp() throws Exception {
        sendOtp(PHONE);

        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, "000000"));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());

        // Attempts should increment
        OtpVerification otp = otpRepository.findLatestActiveByPhone(PHONE).orElseThrow();
        assertThat(otp.getAttempts()).isEqualTo(1);
    }

    @Test
    void verifyOtp_shouldRejectAfterMaxAttempts() throws Exception {
        sendOtp(PHONE);

        String wrongOtpJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, "000000"));

        // Send 5 wrong OTPs
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(wrongOtpJson))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                            .isUnauthorized());
        }

        // 6th attempt (correct OTP this time) should still fail
        String otp = testSmsOtpService.getLastOtp(PHONE);
        String correctOtpJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctOtpJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(429));
    }

    @Test
    void verifyOtp_shouldRejectExpiredOtp() throws Exception {
        sendOtp(PHONE);
        String otp = testSmsOtpService.getLastOtp(PHONE);

        // Artificially expire the OTP
        OtpVerification otpRecord = otpRepository.findLatestByPhoneAndNotExpired(PHONE).orElseThrow();
        otpRecord.setExpiresAt(Instant.now().minusSeconds(1));
        otpRepository.save(otpRecord);

        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(410));
    }

    @Test
    void verifyOtp_shouldRejectWhenNoOtpSent() throws Exception {
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, "123456"));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(410));
    }

    @Test
    void verifyOtp_shouldRejectReusedOtp() throws Exception {
        sendOtp(PHONE);
        String otp = testSmsOtpService.getLastOtp(PHONE);

        // First verification succeeds
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // Reusing same OTP should fail (already expired/verified)
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(410));
    }

    @Test
    void fullFlow_newUserRegistration_shouldWorkEndToEnd() throws Exception {
        // Send OTP
        sendOtp(PHONE);
        String otp = testSmsOtpService.getLastOtp(PHONE);
        assertThat(otp).hasSize(6);

        // Verify OTP
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(PHONE, otp));
        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        VerifyOtpResponse response = objectMapper.readValue(responseJson, VerifyOtpResponse.class);
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();

        String userId = response.user().id();
        assertThat(userId).isNotBlank();

        // Verify DB state
        assertThat(userRepository.findById(java.util.UUID.fromString(userId))).isPresent();
        assertThat(refreshTokenRepository.countByUserIdAndRevokedFalse(java.util.UUID.fromString(userId))).isEqualTo(1);
    }

    // ─── Token Refresh tests ──────────────────────────────────────────

    @Test
    void refresh_shouldIssueNewTokenPair() throws Exception {
        // Register a user first
        String[] tokens = registerAndGetTokens(PHONE);

        // Refresh
        String refreshJson = objectMapper.writeValueAsString(
                new RefreshRequest(tokens[1]));
        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        RefreshResponse response = objectMapper.readValue(responseJson, RefreshResponse.class);
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotEqualTo(tokens[1]); // New refresh token
        assertThat(response.accessExpiresAt()).isNotBlank();
        assertThat(response.refreshExpiresAt()).isNotBlank();

        // Old refresh token should be revoked
        User user = userRepository.findByPhoneNumber(PHONE).orElseThrow();
        long activeTokens = refreshTokenRepository.countByUserIdAndRevokedFalse(user.getId());
        assertThat(activeTokens).isEqualTo(1);
    }

    @Test
    void refresh_shouldRejectInvalidToken() throws Exception {
        String refreshJson = objectMapper.writeValueAsString(
                new RefreshRequest("invalid-token-that-doesnt-exist"));

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void refresh_shouldRejectExpiredToken() throws Exception {
        String[] tokens = registerAndGetTokens(PHONE);

        // Expire the refresh token in DB
        User user = userRepository.findByPhoneNumber(PHONE).orElseThrow();
        java.util.List<RefreshToken> userTokens = refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUserId().equals(user.getId()) && !rt.isRevoked())
                .toList();
        RefreshToken rt = userTokens.get(0);
        rt.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(rt);

        String refreshJson = objectMapper.writeValueAsString(
                new RefreshRequest(tokens[1]));

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void refresh_shouldDetectTheft_whenRevokedTokenReused() throws Exception {
        String[] tokens = registerAndGetTokens(PHONE);
        User user = userRepository.findByPhoneNumber(PHONE).orElseThrow();

        // First refresh (revokes old token)
        String refreshJson = objectMapper.writeValueAsString(
                new RefreshRequest(tokens[1]));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // Reuse the old (now revoked) token — should trigger theft detection
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());

        // All tokens for this user should now be revoked
        long activeTokens = refreshTokenRepository.countByUserIdAndRevokedFalse(user.getId());
        assertThat(activeTokens).isEqualTo(0);
    }

    // ─── Logout tests ─────────────────────────────────────────────────

    @Test
    void logout_shouldRevokeRefreshToken() throws Exception {
        String[] tokens = registerAndGetTokens(PHONE);
        User user = userRepository.findByPhoneNumber(PHONE).orElseThrow();

        String logoutJson = objectMapper.writeValueAsString(
                new LogoutRequest(tokens[1]));

        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        LogoutResponse response = objectMapper.readValue(responseJson, LogoutResponse.class);
        assertThat(response.success()).isTrue();

        // Refresh token should be revoked
        long activeTokens = refreshTokenRepository.countByUserIdAndRevokedFalse(user.getId());
        assertThat(activeTokens).isEqualTo(0);

        // Using it again should fail
        String refreshJson = objectMapper.writeValueAsString(
                new RefreshRequest(tokens[1]));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void logout_withInvalidToken_shouldStillSucceed() throws Exception {
        String logoutJson = objectMapper.writeValueAsString(
                new LogoutRequest("some-random-token"));

        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();

        LogoutResponse response = objectMapper.readValue(responseJson, LogoutResponse.class);
        assertThat(response.success()).isTrue();
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    /**
     * Registers a user via send-otp + verify-otp and returns [accessToken,
     * refreshToken].
     */
    private String[] registerAndGetTokens(String phone) throws Exception {
        sendOtp(phone);
        String otp = testSmsOtpService.getLastOtp(phone);
        String verifyJson = objectMapper.writeValueAsString(new VerifyOtpRequest(phone, otp));
        String responseJson = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        VerifyOtpResponse verifyResponse = objectMapper.readValue(responseJson, VerifyOtpResponse.class);
        return new String[] { verifyResponse.accessToken(), verifyResponse.refreshToken() };
    }

    private void sendOtp(String phone) throws Exception {
        String json = objectMapper.writeValueAsString(new SendOtpRequest(phone));
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}
