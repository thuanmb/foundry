package com.thuanbui.foundry.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.thuanbui.foundry.config.AppProperties;
import com.thuanbui.foundry.config.PollingProperties;
import com.thuanbui.foundry.model.dto.MobileConfigResponse;
import com.thuanbui.foundry.model.dto.PollingConfigDto;
import com.thuanbui.foundry.services.FeatureFlagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
public class MobileConfigController extends BaseApiController {

    private static final String CACHE_KEY = "mobile";

    private final FeatureFlagService featureFlagService;
    private final PollingProperties pollingProperties;
    private final AppProperties appProperties;
    private final Cache<String, MobileConfigResponse> mobileConfigCache;

    public MobileConfigController(
            FeatureFlagService featureFlagService,
            PollingProperties pollingProperties,
            AppProperties appProperties,
            Cache<String, MobileConfigResponse> mobileConfigCache) {
        this.featureFlagService = featureFlagService;
        this.pollingProperties = pollingProperties;
        this.appProperties = appProperties;
        this.mobileConfigCache = mobileConfigCache;
    }

    @GetMapping("/mobile")
    public MobileConfigResponse getMobileConfig() {
        return mobileConfigCache.get(CACHE_KEY, key -> buildConfig());
    }

    private MobileConfigResponse buildConfig() {
        return new MobileConfigResponse(
                featureFlagService.getAllFlags(),
                new PollingConfigDto(
                        pollingProperties.initialDelaySec(),
                        pollingProperties.maxDelaySec(),
                        pollingProperties.backoffMultiplier()),
                appProperties.mobile().minVersion(),
                appProperties.mobile().latestVersion());
    }
}
