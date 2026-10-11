package com.thuanbui.foundry.controller;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.config.SseProperties;
import com.thuanbui.foundry.security.RequestIdentity;
import com.thuanbui.foundry.services.AsyncJobService;
import com.thuanbui.foundry.services.AsyncJobService.JobAccessDeniedException;
import com.thuanbui.foundry.services.AsyncJobService.JobNotFoundException;
import com.thuanbui.foundry.services.JobEventPayloads;
import com.thuanbui.foundry.services.SseSubscriptionRegistry;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE stream for a job. Events: {@code status}, {@code completed}, {@code failed}.
 * Browsers' EventSource cannot set headers, so the token may come via query param (TokenQueryParamFilter).
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobStreamController extends BaseApiController {

    private static final Logger log = LoggerFactory.getLogger(JobStreamController.class);

    private final AsyncJobService asyncJobService;
    private final SseSubscriptionRegistry registry;
    private final SseProperties sseProperties;
    private final ExecutorService sseExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "job-stream-initial-event");
        thread.setDaemon(true);
        return thread;
    });

    public JobStreamController(AsyncJobService asyncJobService,
            SseSubscriptionRegistry registry,
            SseProperties sseProperties) {
        this.asyncJobService = asyncJobService;
        this.registry = registry;
        this.sseProperties = sseProperties;
    }

    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamJobStatus(@PathVariable UUID jobId, HttpServletRequest request) {
        AsyncJob job;
        try {
            job = asyncJobService.getOwnedJob(jobId, RequestIdentity.ownerKey(request));
        } catch (JobNotFoundException e) {
            // Error bodies cannot be negotiated against text/event-stream, so return bare status codes
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (JobAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            SseEmitter immediate = new SseEmitter();
            try {
                immediate.send(terminalEvent(job));
                immediate.complete();
            } catch (Exception e) {
                immediate.completeWithError(e);
            }
            return ResponseEntity.ok(immediate);
        }

        SseEmitter emitter = new SseEmitter(sseProperties.connectionTimeoutSec() * 1000L);
        sseExecutor.submit(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(JobEventPayloads.EVENT_STATUS)
                        .data(JobEventPayloads.status(job)));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        registry.subscribe(jobId, emitter);
        log.info("SSE connection established for job {}", jobId);
        return ResponseEntity.ok(emitter);
    }

    private SseEmitter.SseEventBuilder terminalEvent(AsyncJob job) {
        if (job.getStatus() == JobStatus.COMPLETED) {
            return SseEmitter.event().name(JobEventPayloads.EVENT_COMPLETED).data(JobEventPayloads.completed(job));
        }
        return SseEmitter.event().name(JobEventPayloads.EVENT_FAILED).data(JobEventPayloads.failed(job));
    }

    @PreDestroy
    void shutdown() {
        sseExecutor.shutdownNow();
    }
}
