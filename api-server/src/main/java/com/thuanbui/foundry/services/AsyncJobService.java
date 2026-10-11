package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.WorkerProperties;
import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.util.ParametersHashUtil;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsyncJobService {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobService.class);
    private static final String RID_KEY = "rid";

    private final AsyncJobRepository jobRepository;
    private final PostgresNotifyService notifyService;
    private final WorkerProperties workerProperties;

    public AsyncJobService(AsyncJobRepository jobRepository,
            PostgresNotifyService notifyService,
            WorkerProperties workerProperties) {
        this.jobRepository = jobRepository;
        this.notifyService = notifyService;
        this.workerProperties = workerProperties;
    }

    /**
     * Creates a job (or returns an identical in-flight one owned by the same caller) and
     * notifies workers. {@code ownerKey} is {@code user:<uuid>} or {@code anon:<token-hash>}.
     */
    @Transactional
    public AsyncJob createJob(JobType jobType, String jobName, Map<String, Object> parameters,
            String ownerKey, Integer estimatedDurationSec, int priority, int maxRetries) {
        String parametersHash = computeDedupHash(jobType, ownerKey, parameters);

        Optional<AsyncJob> existing = jobRepository
                .findByParametersHashAndStatusInAndDeletedFalse(parametersHash,
                        List.of(JobStatus.PENDING, JobStatus.RUNNING));
        if (existing.isPresent()) {
            return existing.get();
        }

        AsyncJob job = new AsyncJob();
        job.setJobType(jobType);
        job.setJobName(jobName);
        job.setParameters(parameters);
        job.setParametersHash(parametersHash);
        job.setOwnerKey(ownerKey);
        job.setEstimatedDurationSec(estimatedDurationSec);
        job.setStatus(JobStatus.PENDING);
        job.setPriority(priority);
        job.setMaxRetries(maxRetries);
        job.setRetryCount(0);
        job.setDeleted(false);
        job.setCorrelationId(MDC.get(RID_KEY));

        AsyncJob saved = jobRepository.save(job);
        notifyService.notify(workerProperties.listen().channel(), saved.getId().toString());
        log.info("Created job {} of type {} with hash {}", saved.getId(), jobType, parametersHash);
        return saved;
    }

    @Transactional
    public Optional<AsyncJob> findActiveByHash(String parametersHash) {
        return jobRepository.findByParametersHashAndStatusInAndDeletedFalse(
                parametersHash, List.of(JobStatus.PENDING, JobStatus.RUNNING));
    }

    @Transactional
    public void updateStatus(UUID jobId, JobStatus newStatus, String message) {
        AsyncJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(newStatus);
        job.setMessage(message);
        job.setUpdatedAt(Instant.now());
        if (newStatus == JobStatus.RUNNING) {
            job.setStartedAt(Instant.now());
        }
        if (newStatus == JobStatus.COMPLETED || newStatus == JobStatus.FAILED) {
            job.setCompletedAt(Instant.now());
        }
        jobRepository.save(job);
        log.info("Job {} status updated to {}", jobId, newStatus);
    }

    @Transactional
    public void heartbeat(UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
        });
    }

    @Transactional
    public int resetStaleJobs() {
        int staleTimeoutMinutes = workerProperties.job().staleTimeoutMinutes();
        Instant staleThreshold = Instant.now().minusSeconds((long) staleTimeoutMinutes * 60);
        int reset = jobRepository.resetStaleJobs(staleThreshold);
        if (reset > 0) {
            log.info("Reset {} stale jobs back to PENDING", reset);
        }
        return reset;
    }

    public Optional<AsyncJob> findById(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    /** Loads a job the caller owns; unknown or deleted → 404, different owner → 403. */
    public AsyncJob getOwnedJob(UUID jobId, String ownerKey) {
        AsyncJob job = jobRepository.findById(jobId)
                .filter(j -> !j.isDeleted())
                .orElseThrow(() -> new JobNotFoundException(jobId));
        if (ownerKey == null || !ownerKey.equals(job.getOwnerKey())) {
            throw new JobAccessDeniedException(jobId);
        }
        return job;
    }

    public static class JobNotFoundException extends RuntimeException {
        public JobNotFoundException(UUID jobId) {
            super("Job not found: " + jobId);
        }
    }

    public static class JobAccessDeniedException extends RuntimeException {
        public JobAccessDeniedException(UUID jobId) {
            super("Access denied to job: " + jobId);
        }
    }

    static String computeDedupHash(JobType jobType, String ownerKey, Map<String, Object> parameters) {
        Map<String, Object> hashInput = new TreeMap<>();
        hashInput.put("jobType", jobType.name());
        hashInput.put("ownerKey", ownerKey);
        hashInput.put("parameters", new TreeMap<>(parameters));
        return ParametersHashUtil.compute(hashInput);
    }
}
