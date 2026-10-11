package com.thuanbui.foundry.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bound from {@code feature.flags.<key>.enabled=true|false}. */
@ConfigurationProperties(prefix = "feature")
public record FeatureProperties(Map<String, Toggle> flags) {

    public FeatureProperties {
        flags = flags == null ? Map.of() : Map.copyOf(flags);
    }

    public record Toggle(boolean enabled) {
    }
}
