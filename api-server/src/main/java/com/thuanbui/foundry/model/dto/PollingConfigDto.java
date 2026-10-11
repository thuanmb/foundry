package com.thuanbui.foundry.model.dto;

public record PollingConfigDto(
        int initialDelaySec,
        int maxDelaySec,
        double backoffMultiplier) {
}
