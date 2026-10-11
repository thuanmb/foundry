package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.model.dto.LogoutRequest;
import com.thuanbui.foundry.model.dto.LogoutResponse;
import com.thuanbui.foundry.model.dto.RefreshRequest;
import com.thuanbui.foundry.model.dto.RefreshResponse;
import com.thuanbui.foundry.model.dto.SendOtpRequest;
import com.thuanbui.foundry.model.dto.SendOtpResponse;
import com.thuanbui.foundry.model.dto.VerifyOtpRequest;
import com.thuanbui.foundry.model.dto.VerifyOtpResponse;
import com.thuanbui.foundry.model.dto.VerifyFirebaseTokenRequest;
import com.thuanbui.foundry.services.AuthService;
import com.thuanbui.foundry.services.RefreshTokenService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends BaseApiController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        SendOtpResponse response = authService.sendOtp(request.phoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = authService.verifyOtp(request.phoneNumber(), request.otp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-firebase-token")
    public ResponseEntity<VerifyOtpResponse> verifyFirebaseToken(
            @Valid @RequestBody VerifyFirebaseTokenRequest request) {
        VerifyOtpResponse response = authService.verifyFirebaseToken(request.idToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.TokenPair tokens = refreshTokenService.rotateToken(request.refreshToken());
        return ResponseEntity.ok(new RefreshResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.accessExpiresAt().toString(),
                tokens.refreshExpiresAt().toString()));
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revokeToken(request.refreshToken());
        return ResponseEntity.ok(new LogoutResponse(true));
    }
}
