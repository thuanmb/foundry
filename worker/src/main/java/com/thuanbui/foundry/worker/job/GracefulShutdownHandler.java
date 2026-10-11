package com.thuanbui.foundry.worker.job;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;

import jakarta.annotation.PreDestroy;

@Component
public class GracefulShutdownHandler {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private final AsyncJobRepository asyncJobRepository;
    private final WorkerProperties workerProperties;
    private final AtomicReference<AsyncJob> currentJob = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> completionLatch = new AtomicReference<>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private final AtomicBoolean currentJobReleased = new AtomicBoolean(false);

    public GracefulShutdownHandler(AsyncJobRepository asyncJobRepository,
            WorkerProperties workerProperties) {
        this.asyncJobRepository = asyncJobRepository;
        this.workerProperties = workerProperties;
    }

    @PreDestroy
    public void onShutdown() {
        shuttingDown.set(true);

        AsyncJob job = currentJob.get();
        if (job == null) {
            log.info("No active job, shutting down immediately");
            return;
        }

        Duration elapsed = Duration.between(job.getStartedAt(), Instant.now());
        int estimatedTotal = job.getEstimatedDurationSec() != null
                ? job.getEstimatedDurationSec()
                : 120;
        long remainingSec = estimatedTotal - elapsed.toSeconds();

        int threshold = workerProperties.job().gracefulShutdownThresholdSec();

        if (remainingSec <= threshold) {
            log.info("Job {} nearly done ({} sec remaining, threshold {} sec), finishing...",
                    job.getId(), remainingSec, threshold);
            waitForCompletion();
        } else {
            log.info("Job {} has {} sec remaining (threshold {} sec), releasing back to PENDING",
                    job.getId(), remainingSec, threshold);
            releaseJob(job);
        }
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * True once shutdown released the running job back to PENDING; long-running
     * processors should stop.
     */
    public boolean isCurrentJobReleased() {
        return currentJobReleased.get();
    }

    public void jobStarted(AsyncJob job) {
        currentJobReleased.set(false);
        currentJob.set(job);
        completionLatch.set(new CountDownLatch(1));
    }

    public void jobCompleted() {
        currentJob.set(null);
        CountDownLatch latch = completionLatch.getAndSet(null);
        if (latch != null) {
            latch.countDown();
        }
    }

    private void waitForCompletion() {
        CountDownLatch latch = completionLatch.get();
        if (latch == null) {
            return;
        }
        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                log.warn("Timed out waiting for job to complete");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for job completion");
        }
    }

    private void releaseJob(AsyncJob job) {
        currentJobReleased.set(true);
        job.setStatus(JobStatus.PENDING);
        job.setMessage("Released by worker shutdown");
        job.setStartedAt(null);
        asyncJobRepository.save(job);
        log.info("Released job {} back to PENDING", job.getId());
    }
}
