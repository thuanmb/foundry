package com.thuanbui.foundry.model.dto;

import com.thuanbui.foundry.common.model.entity.User;

public record UserProfileResponse(
        String id,
        String phoneNumber,
        String displayName,
        String createdAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
    }
}
