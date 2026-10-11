package com.thuanbui.foundry.worker.job;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;

@Component
public class HeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final AsyncJobRepository asyncJobRepository;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final TransactionTemplate transactionTemplate;
    private final int heartbeatIntervalSec;
    private final Map<UUID, ScheduledFuture<?>> activeHeartbeats = new ConcurrentHashMap<>();

    public HeartbeatScheduler(AsyncJobRepository asyncJobRepository,
            ThreadPoolTaskScheduler taskScheduler,
            TransactionTemplate transactionTemplate,
            WorkerProperties workerProperties) {
        this.asyncJobRepository = asyncJobRepository;
        this.taskScheduler = taskScheduler;
        this.transactionTemplate = transactionTemplate;
        this.heartbeatIntervalSec = workerProperties.job().heartbeatIntervalSec();
    }

    public void startHeartbeat(UUID jobId) {
        var future = taskScheduler.scheduleAtFixedRate(
                () -> updateHeartbeat(jobId),
                java.time.Duration.ofSeconds(heartbeatIntervalSec));
        activeHeartbeats.put(jobId, future);
        log.debug("Started heartbeat for job {}", jobId);
    }

    public void stopHeartbeat(UUID jobId) {
        var future = activeHeartbeats.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.debug("Stopped heartbeat for job {}", jobId);
        }
    }

    private void updateHeartbeat(UUID jobId) {
        try {
            transactionTemplate.executeWithoutResult(status -> asyncJobRepository.updateHeartbeat(jobId));
            log.debug("Heartbeat updated for job {}", jobId);
        } catch (Exception e) {
            log.warn("Failed to update heartbeat for job {}: {}", jobId, e.getMessage());
        }
    }

    public int activeCount() {
        return activeHeartbeats.size();
    }
}
