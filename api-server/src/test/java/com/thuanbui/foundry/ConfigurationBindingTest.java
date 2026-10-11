package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thuanbui.foundry.config.AccountProperties;
import com.thuanbui.foundry.config.AppProperties;
import com.thuanbui.foundry.config.AuthProperties;
import com.thuanbui.foundry.config.CacheProperties;
import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.config.PollingProperties;
import com.thuanbui.foundry.config.SecurityProperties;
import com.thuanbui.foundry.config.SseProperties;
import com.thuanbui.foundry.config.WorkerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = ConfigurationBindingTest.TestConfig.class)
@TestPropertySource(properties = {
        "auth.access-token.ttl-minutes=60",
        "auth.access-token.issuer=foundry",
        "auth.refresh-token.ttl-days=30",
        "auth.refresh-token.max-per-user=5",
        "auth.otp.length=6",
        "auth.otp.expiry-minutes=5",
        "auth.otp.max-attempts=5",
        "auth.otp.rate-limit-per-hour=3",
        "auth.otp.cooldown-sec=60",
        "auth.jwe.ttl-hours=24",
        "auth.jwe.algorithm=A256GCM",
        "worker.job.max-retries=3",
        "worker.job.heartbeat-interval-sec=30",
        "worker.job.stale-timeout-minutes=5",
        "worker.job.stale-check-interval-sec=60",
        "worker.job.graceful-shutdown-threshold-sec=10",
        "worker.listen.channel=new_job",
        "worker.listen.complete-channel=job_completed",
        "worker.listen.fallback-poll-sec=30",
        "cache.caffeine.features-ttl-sec=30",
        "cache.caffeine.max-entries=1000",
        "cache.caffeine.mobile-config-ttl-sec=30",
        "security.rate-limit.public-endpoints-per-sec=10",
        "security.rate-limit.auth-endpoints-per-min=20",
        "security.request.max-body-size-mb=10",
        "security.cors.allowed-origins=*",
        "security.cors.max-age-sec=3600",
        "sse.heartbeat-interval-sec=15",
        "sse.connection-timeout-sec=300",
        "polling.initial-delay-sec=2",
        "polling.max-delay-sec=15",
        "polling.backoff-multiplier=2.0",
        "feature.flags.notes.enabled=true",
        "feature.flags.echo.enabled=false",
        "feature.flags.anonymousWeb.enabled=true",
        "account.deletion.grace-period-days=30",
        "account.deletion.anonymize-on-delete=true",
        "account.retention.inactive-days=365",
        "app.mobile.min-version=1.0.0",
        "app.mobile.latest-version=1.0.0"
})
class ConfigurationBindingTest {

    @Configuration
    @EnableConfigurationProperties({
            AuthProperties.class,
            WorkerProperties.class,
            CacheProperties.class,
            SecurityProperties.class,
            SseProperties.class,
            FeatureProperties.class,
            AccountProperties.class,
            AppProperties.class,
            PollingProperties.class
    })
    static class TestConfig {
    }

    @Autowired
    AuthProperties authProperties;
    @Autowired
    WorkerProperties workerProperties;
    @Autowired
    CacheProperties cacheProperties;
    @Autowired
    SecurityProperties securityProperties;
    @Autowired
    SseProperties sseProperties;
    @Autowired
    FeatureProperties featureProperties;
    @Autowired
    AccountProperties accountProperties;
    @Autowired
    AppProperties appProperties;
    @Autowired
    PollingProperties pollingProperties;

    @Test
    void allConfigBeansNotNull() {
        assertNotNull(authProperties);
        assertNotNull(workerProperties);
        assertNotNull(cacheProperties);
        assertNotNull(securityProperties);
        assertNotNull(sseProperties);
        assertNotNull(featureProperties);
        assertNotNull(accountProperties);
        assertNotNull(appProperties);
        assertNotNull(pollingProperties);
    }

    @Test
    void authDefaults() {
        assertEquals(60, authProperties.accessToken().ttlMinutes());
        assertEquals("foundry", authProperties.accessToken().issuer());
        assertEquals(30, authProperties.refreshToken().ttlDays());
        assertEquals(6, authProperties.otp().length());
        assertEquals(24, authProperties.jwe().ttlHours());
        assertEquals("A256GCM", authProperties.jwe().algorithm());
    }

    @Test
    void workerDefaults() {
        assertEquals(3, workerProperties.job().maxRetries());
        assertEquals("new_job", workerProperties.listen().channel());
        assertEquals("job_completed", workerProperties.listen().completeChannel());
    }

    @Test
    void cacheDefaults() {
        assertEquals(30, cacheProperties.caffeine().featuresTtlSec());
        assertEquals(1000, cacheProperties.caffeine().maxEntries());
        assertEquals(30, cacheProperties.caffeine().mobileConfigTtlSec());
    }

    @Test
    void pollingDefaults() {
        assertEquals(2, pollingProperties.initialDelaySec());
        assertEquals(15, pollingProperties.maxDelaySec());
        assertEquals(2.0, pollingProperties.backoffMultiplier());
    }

    @Test
    void securityDefaults() {
        assertEquals(10, securityProperties.rateLimit().publicEndpointsPerSec());
        assertEquals(10, securityProperties.request().maxBodySizeMb());
        assertEquals("*", securityProperties.cors().allowedOrigins());
    }

    @Test
    void featureFlagsBindAsMap() {
        assertEquals(3, featureProperties.flags().size());
        assertTrue(featureProperties.flags().get("notes").enabled());
        assertFalse(featureProperties.flags().get("echo").enabled());
        assertTrue(featureProperties.flags().get("anonymousWeb").enabled());
    }

    @Test
    void accountDefaults() {
        assertEquals(30, accountProperties.deletion().gracePeriodDays());
        assertTrue(accountProperties.deletion().anonymizeOnDelete());
    }

    @Test
    void appDefaults() {
        assertEquals("1.0.0", appProperties.mobile().minVersion());
        assertEquals("1.0.0", appProperties.mobile().latestVersion());
    }
}
