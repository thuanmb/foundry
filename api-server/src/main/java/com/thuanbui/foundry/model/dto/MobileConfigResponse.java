package com.thuanbui.foundry.model.dto;

import java.util.Map;

public record MobileConfigResponse(
        Map<String, Boolean> features,
        PollingConfigDto pollingConfig,
        String minAppVersion,
        String latestAppVersion) {
}
