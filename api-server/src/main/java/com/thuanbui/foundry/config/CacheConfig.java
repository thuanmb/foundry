package com.thuanbui.foundry.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thuanbui.foundry.model.dto.MobileConfigResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    private final CacheProperties cacheProperties;

    public CacheConfig(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    @Bean
    public Cache<String, Map<String, Boolean>> featuresCaffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(Duration.ofSeconds(cacheProperties.caffeine().featuresTtlSec()))
                .build();
    }

    @Bean
    public Cache<String, MobileConfigResponse> mobileConfigCaffeineCache() {
        return Caffeine.newBuilder()
                .maximumSize(Math.min(10, cacheProperties.caffeine().maxEntries()))
                .expireAfterWrite(Duration.ofSeconds(cacheProperties.caffeine().mobileConfigTtlSec()))
                .build();
    }
}
