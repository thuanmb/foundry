package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.FeatureProperties;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final FeatureProperties featureProperties;

    public FeatureFlagService(FeatureProperties featureProperties) {
        this.featureProperties = featureProperties;
    }

    public boolean isEnabled(String feature) {
        FeatureProperties.Toggle toggle = featureProperties.flags().get(feature);
        return toggle != null && toggle.enabled();
    }

    public Map<String, Boolean> getAllFlags() {
        Map<String, Boolean> flags = new TreeMap<>();
        featureProperties.flags().forEach((key, toggle) -> flags.put(key, toggle != null && toggle.enabled()));
        return flags;
    }
}
