package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thuanbui.foundry.config.AppProperties;
import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.config.PollingProperties;
import com.thuanbui.foundry.controller.MobileConfigController;
import com.thuanbui.foundry.model.dto.MobileConfigResponse;
import com.thuanbui.foundry.services.FeatureFlagService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MobileConfigControllerTest {

    private MobileConfigController controller;

    @BeforeEach
    void setUp() {
        FeatureProperties featureProperties = new FeatureProperties(Map.of(
                "notes", new FeatureProperties.Toggle(true),
                "echo", new FeatureProperties.Toggle(false)));
        FeatureFlagService featureFlagService = new FeatureFlagService(featureProperties);

        PollingProperties pollingProperties = new PollingProperties(2, 15, 2.0);
        AppProperties appProperties = new AppProperties(new AppProperties.Mobile("1.0.0", "1.2.0"));
        Cache<String, MobileConfigResponse> cache = Caffeine.newBuilder().maximumSize(10).build();

        controller = new MobileConfigController(featureFlagService, pollingProperties, appProperties, cache);
    }

    @Test
    void getMobileConfig_shouldPopulateAllFields() {
        MobileConfigResponse response = controller.getMobileConfig();

        assertEquals(2, response.features().size());
        assertTrue(response.features().get("notes"));
        assertFalse(response.features().get("echo"));
        assertEquals(2, response.pollingConfig().initialDelaySec());
        assertEquals(15, response.pollingConfig().maxDelaySec());
        assertEquals(2.0, response.pollingConfig().backoffMultiplier());
        assertEquals("1.0.0", response.minAppVersion());
        assertEquals("1.2.0", response.latestAppVersion());
    }

    @Test
    void getMobileConfig_shouldBeCached() {
        MobileConfigResponse first = controller.getMobileConfig();
        MobileConfigResponse second = controller.getMobileConfig();

        assertSame(first, second);
    }
}
