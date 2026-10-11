package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.services.FeatureFlagService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeatureFlagServiceTest {

    private FeatureFlagService service;

    @BeforeEach
    void setUp() {
        FeatureProperties props = new FeatureProperties(Map.of(
                "notes", new FeatureProperties.Toggle(true),
                "echo", new FeatureProperties.Toggle(false),
                "anonymousWeb", new FeatureProperties.Toggle(true)));
        service = new FeatureFlagService(props);
    }

    @Test
    void isEnabled_shouldReturnCorrectFlag() {
        assertTrue(service.isEnabled("notes"));
        assertFalse(service.isEnabled("echo"));
        assertTrue(service.isEnabled("anonymousWeb"));
        assertFalse(service.isEnabled("unknown"));
    }

    @Test
    void getAllFlags_shouldReturnAllFlags() {
        Map<String, Boolean> flags = service.getAllFlags();
        assertEquals(3, flags.size());
        assertFalse(flags.get("echo"));
        assertTrue(flags.get("notes"));
    }

    @Test
    void emptyProperties_shouldDisableEverything() {
        FeatureFlagService empty = new FeatureFlagService(new FeatureProperties(null));
        assertFalse(empty.isEnabled("notes"));
        assertTrue(empty.getAllFlags().isEmpty());
    }
}
