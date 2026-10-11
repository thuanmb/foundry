package com.thuanbui.foundry.worker.job;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;
import com.thuanbui.foundry.worker.listener.NotifyService;
import com.thuanbui.foundry.worker.llm.NonRetryableException;
import com.thuanbui.foundry.worker.llm.RetryableException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final AsyncJobRepository asyncJobRepository;
    private final JobDispatcher jobDispatcher;
    private final HeartbeatScheduler heartbeatScheduler;
    private final NotifyService notifyService;
    private final GracefulShutdownHandler gracefulShutdownHandler;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;
    private final WorkerProperties workerProperties;

    public JobExecutionService(AsyncJobRepository asyncJobRepository,
            JobDispatcher jobDispatcher,
            HeartbeatScheduler heartbeatScheduler,
            NotifyService notifyService,
            GracefulShutdownHandler gracefulShutdownHandler,
            TransactionTemplate transactionTemplate,
            MeterRegistry meterRegistry,
            WorkerProperties workerProperties) {
        this.asyncJobRepository = asyncJobRepository;
        this.jobDispatcher = jobDispatcher;
        this.heartbeatScheduler = heartbeatScheduler;
        this.notifyService = notifyService;
        this.gracefulShutdownHandler = gracefulShutdownHandler;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
        this.workerProperties = workerProperties;
    }

    public void executeJob(AsyncJob job) {
        UUID jobId = job.getId();
        Timer.Sample sample = Timer.start(meterRegistry);
        if (job.getCorrelationId() != null) {
            MDC.put("correlationId", job.getCorrelationId());
        }
        MDC.put("jobId", jobId.toString());
        gracefulShutdownHandler.jobStarted(job);

        try {
            heartbeatScheduler.startHeartbeat(jobId);
            Map<String, Object> result = jobDispatcher.dispatch(job);
            onSuccess(job, result != null ? result : Map.of());
        } catch (RetryableException e) {
            onRetryableFailure(job, e);
        } catch (NonRetryableException e) {
            log.error("Non-retryable error for job {}: {}", jobId, e.getMessage());
            onPermanentFailure(job, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error for job {}", jobId, e);
            onRetryableFailure(job, e);
        } finally {
            heartbeatScheduler.stopHeartbeat(jobId);
            gracefulShutdownHandler.jobCompleted();
            sample.stop(meterRegistry.timer("worker_job_duration_seconds", "type", job.getJobType().name()));
            MDC.remove("correlationId");
            MDC.remove("jobId");
        }
    }

    private void onSuccess(AsyncJob job, Map<String, Object> result) {
        UUID jobId = job.getId();
        meterRegistry.counter("worker_jobs_processed_total",
                "type", job.getJobType().name(), "status", "completed").increment();

        Instant now = Instant.now();
        job.setResult(result);
        job.setStatus(JobStatus.COMPLETED);
        job.setMessage(null);
        job.setCompletedAt(now);
        job.setUpdatedAt(now);

        transactionTemplate.executeWithoutResult(status -> asyncJobRepository.save(job));

        notifyService.notify(workerProperties.listen().completeChannel(), jobId.toString());
        log.info("Job {} completed successfully", jobId);
    }

    private void onRetryableFailure(AsyncJob job, Exception e) {
        UUID jobId = job.getId();
        int nextRetry = job.getRetryCount() + 1;
        int maxRetries = job.getMaxRetries();

        if (nextRetry <= maxRetries) {
            log.warn("Retryable error for job {} (attempt {}/{}): {}",
                    jobId, nextRetry, maxRetries, e.getMessage());
            job.setStatus(JobStatus.PENDING);
            job.setRetryCount(nextRetry);
            job.setMessage(e.getMessage());
            job.setUpdatedAt(Instant.now());
            transactionTemplate.executeWithoutResult(status -> {
                asyncJobRepository.save(job);
            });
        } else {
            log.error("Job {} exceeded max retries ({}): {}",
                    jobId, maxRetries, e.getMessage());
            onPermanentFailure(job, "Max retries exceeded (" + maxRetries + "): " + e.getMessage());
        }
    }

    private void onPermanentFailure(AsyncJob job, String message) {
        meterRegistry.counter("worker_jobs_processed_total",
                "type", job.getJobType().name(), "status", "failed").increment();

        Instant now = Instant.now();
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(now);
        job.setUpdatedAt(now);
        job.setMessage(message);

        transactionTemplate.executeWithoutResult(status -> asyncJobRepository.save(job));

        notifyService.notify(workerProperties.listen().completeChannel(), job.getId().toString());
        log.error("Job {} permanently failed: {}", job.getId(), message);
    }
}
