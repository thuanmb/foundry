package com.thuanbui.foundry.model.dto;

public record DeleteAccountResponse(
        boolean success,
        String deletionScheduledAt) {
}
