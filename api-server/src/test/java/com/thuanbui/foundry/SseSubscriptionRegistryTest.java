package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thuanbui.foundry.services.SseSubscriptionRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseSubscriptionRegistryTest {

    private final SseSubscriptionRegistry registry = new SseSubscriptionRegistry(new SimpleMeterRegistry());

    @Test
    void shouldTrackSubscriptions() {
        UUID jobId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        registry.subscribe(jobId, emitter);
        assertEquals(1, registry.getSubscribers(jobId).size());
    }

    @Test
    void shouldRemoveSubscription() {
        UUID jobId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        registry.subscribe(jobId, emitter);
        registry.remove(jobId, emitter);
        assertTrue(registry.getSubscribers(jobId).isEmpty());
    }

    @Test
    void shouldReturnEmptyForUnknownJob() {
        assertTrue(registry.getSubscribers(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldSupportMultipleSubscribers() {
        UUID jobId = UUID.randomUUID();
        SseEmitter e1 = new SseEmitter();
        SseEmitter e2 = new SseEmitter();
        registry.subscribe(jobId, e1);
        registry.subscribe(jobId, e2);
        assertEquals(2, registry.getSubscribers(jobId).size());
    }
}
