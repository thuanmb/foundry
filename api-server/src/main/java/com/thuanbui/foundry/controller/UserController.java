package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.model.dto.DeleteAccountRequest;
import com.thuanbui.foundry.model.dto.DeleteAccountResponse;
import com.thuanbui.foundry.model.dto.UpdateProfileRequest;
import com.thuanbui.foundry.model.dto.UserProfileResponse;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.security.UnauthorizedException;
import com.thuanbui.foundry.services.UserProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController extends BaseApiController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest request) {
        return ResponseEntity.ok(userProfileService.getProfile(requireUserId(request)));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(requireUserId(request), updateRequest));
    }

    @DeleteMapping("/account")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest deleteRequest,
            HttpServletRequest request) {
        return ResponseEntity.ok(userProfileService.deleteAccount(requireUserId(request), deleteRequest.otp()));
    }

    private static UUID requireUserId(HttpServletRequest request) {
        UUID userId = RequestIdentity.userId(request);
        if (userId == null) {
            throw new UnauthorizedException();
        }
        return userId;
    }
}
