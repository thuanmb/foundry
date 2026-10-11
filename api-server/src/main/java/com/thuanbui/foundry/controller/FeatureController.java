package com.thuanbui.foundry.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.thuanbui.foundry.services.FeatureFlagService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/features")
public class FeatureController extends BaseApiController {

    private final FeatureFlagService featureFlagService;
    private final Cache<String, Map<String, Boolean>> featuresCache;

    public FeatureController(FeatureFlagService featureFlagService,
            Cache<String, Map<String, Boolean>> featuresCache) {
        this.featureFlagService = featureFlagService;
        this.featuresCache = featuresCache;
    }

    @GetMapping
    public Map<String, Boolean> getFeatures() {
        return featuresCache.get("all", key -> featureFlagService.getAllFlags());
    }
}
