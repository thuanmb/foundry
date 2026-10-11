package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.SseProperties;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(SseHeartbeatScheduler.class);

    private final SseSubscriptionRegistry registry;

    public SseHeartbeatScheduler(SseSubscriptionRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${sse.heartbeat-interval-sec}000")
    public void sendHeartbeat() {
        for (SseEmitter emitter : registry.getAllEmitters()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(Map.of("timestamp", Instant.now().toString())));
            } catch (Exception e) {
                // Emitter will be cleaned up via onError callback in registry
            }
        }
    }
}
