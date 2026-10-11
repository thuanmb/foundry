package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.controller.FeatureController;
import com.thuanbui.foundry.services.FeatureFlagService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureControllerTest {

    private FeatureController controller;

    @BeforeEach
    void setUp() {
        FeatureProperties props = new FeatureProperties(Map.of(
                "notes", new FeatureProperties.Toggle(true),
                "echo", new FeatureProperties.Toggle(false)));
        FeatureFlagService service = new FeatureFlagService(props);
        Cache<String, Map<String, Boolean>> cache = Caffeine.newBuilder()
                .maximumSize(1)
                .build();
        controller = new FeatureController(service, cache);
    }

    @Test
    void getFeatures_shouldReturnAllFlags() {
        Map<String, Boolean> features = controller.getFeatures();
        assertEquals(2, features.size());
        assertTrue(features.get("notes"));
        assertFalse(features.get("echo"));
    }

    @Test
    void getFeatures_shouldBeCached() {
        Map<String, Boolean> first = controller.getFeatures();
        Map<String, Boolean> second = controller.getFeatures();
        // Same reference since Caffeine caches it
        assertEquals(first, second);
    }
}
