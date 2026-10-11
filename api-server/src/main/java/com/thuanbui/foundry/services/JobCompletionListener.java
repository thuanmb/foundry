package com.thuanbui.foundry.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.config.WorkerProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class JobCompletionListener extends PostgresNotificationListener {

    private final SseSubscriptionRegistry registry;
    private final WorkerProperties workerProperties;
    private final ObjectMapper objectMapper;
    private final AsyncJobRepository jobRepository;

    public JobCompletionListener(Environment environment,
            SseSubscriptionRegistry registry,
            WorkerProperties workerProperties,
            ObjectMapper objectMapper,
            AsyncJobRepository jobRepository) {
        super(environment);
        this.registry = registry;
        this.workerProperties = workerProperties;
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
    }

    @Override
    protected String channel() {
        return workerProperties.listen().completeChannel();
    }

    @Override
    protected void handleNotification(String payload) {
        try {
            UUID jobId = UUID.fromString(payload.trim());
            log.info("Received job completion notification for {}", jobId);

            AsyncJob job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                log.warn("Job {} not found after notification", jobId);
                return;
            }

            if (job.getStatus() == JobStatus.COMPLETED) {
                pushToSubscribers(jobId, JobEventPayloads.EVENT_COMPLETED, JobEventPayloads.completed(job));
            } else if (job.getStatus() == JobStatus.FAILED) {
                pushToSubscribers(jobId, JobEventPayloads.EVENT_FAILED, JobEventPayloads.failed(job));
            } else {
                log.info("Job {} status is {} — not sending completion event", jobId, job.getStatus());
            }
        } catch (Exception e) {
            log.error("Failed to handle notification: {}", payload, e);
        }
    }

    private void pushToSubscribers(UUID jobId, String eventName, Map<String, Object> data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Failed to serialize event data for job {}", jobId, e);
            return;
        }

        for (SseEmitter emitter : registry.getSubscribers(jobId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(json));
                emitter.complete();
            } catch (Exception e) {
                log.debug("Failed to send SSE event for job {}", jobId, e);
            }
            registry.remove(jobId, emitter);
        }
    }
}
