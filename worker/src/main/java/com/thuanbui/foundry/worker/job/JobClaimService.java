package com.thuanbui.foundry.worker.job;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;

@Service
public class JobClaimService {

    private static final Logger log = LoggerFactory.getLogger(JobClaimService.class);

    private final AsyncJobRepository asyncJobRepository;
    private final JobExecutionService jobExecutionService;
    private final GracefulShutdownHandler gracefulShutdownHandler;
    private final TransactionTemplate transactionTemplate;

    public JobClaimService(AsyncJobRepository asyncJobRepository,
            JobExecutionService jobExecutionService,
            GracefulShutdownHandler gracefulShutdownHandler,
            TransactionTemplate transactionTemplate) {
        this.asyncJobRepository = asyncJobRepository;
        this.jobExecutionService = jobExecutionService;
        this.gracefulShutdownHandler = gracefulShutdownHandler;
        this.transactionTemplate = transactionTemplate;
    }

    public void tryClaimAndProcess() {
        if (gracefulShutdownHandler.isShuttingDown()) {
            log.debug("Shutting down, skipping job claim");
            return;
        }

        AsyncJob job = transactionTemplate.execute(status -> asyncJobRepository.claimNextJob()
                .map(this::markRunning)
                .orElse(null));

        if (job != null) {
            jobExecutionService.executeJob(job);
        } else {
            log.debug("No pending jobs available");
        }
    }

    private AsyncJob markRunning(AsyncJob job) {
        log.info("Claimed job {} of type {}", job.getId(), job.getJobType());
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        return asyncJobRepository.save(job);
    }
}
