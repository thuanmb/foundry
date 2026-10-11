package com.thuanbui.foundry.services;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseSubscriptionRegistry {

    private final ConcurrentHashMap<UUID, List<SseEmitter>> subscriptions = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public SseSubscriptionRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("sse_connections_active", this, SseSubscriptionRegistry::totalEmitters)
                .description("Number of active SSE connections")
                .register(meterRegistry);
    }

    public void subscribe(UUID jobId, SseEmitter emitter) {
        subscriptions.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(jobId, emitter));
        emitter.onTimeout(() -> remove(jobId, emitter));
        emitter.onError(e -> remove(jobId, emitter));
    }

    public List<SseEmitter> getSubscribers(UUID jobId) {
        return subscriptions.getOrDefault(jobId, List.of());
    }

    public void remove(UUID jobId, SseEmitter emitter) {
        List<SseEmitter> emitters = subscriptions.get(jobId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                subscriptions.remove(jobId);
            }
        }
    }

    public List<SseEmitter> getAllEmitters() {
        List<SseEmitter> all = new ArrayList<>();
        for (List<SseEmitter> emitters : subscriptions.values()) {
            all.addAll(emitters);
        }
        return all;
    }

    private long totalEmitters() {
        long total = 0;
        for (List<SseEmitter> emitters : subscriptions.values()) {
            total += emitters.size();
        }
        return total;
    }
}
