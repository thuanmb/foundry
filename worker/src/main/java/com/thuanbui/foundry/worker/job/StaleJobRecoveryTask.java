package com.thuanbui.foundry.worker.job;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thuanbui.foundry.common.repository.AsyncJobRepository;
import com.thuanbui.foundry.worker.config.WorkerProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class StaleJobRecoveryTask {

    private static final Logger log = LoggerFactory.getLogger(StaleJobRecoveryTask.class);

    private final AsyncJobRepository asyncJobRepository;
    private final WorkerProperties workerProperties;
    private final Counter recoveredCounter;

    public StaleJobRecoveryTask(AsyncJobRepository asyncJobRepository,
            WorkerProperties workerProperties,
            MeterRegistry meterRegistry) {
        this.asyncJobRepository = asyncJobRepository;
        this.workerProperties = workerProperties;
        this.recoveredCounter = Counter.builder("worker_jobs_recovered_total")
                .description("Number of stale jobs recovered")
                .register(meterRegistry);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${worker.job.stale-check-interval-sec:60}000")
    public int recoverStaleJobs() {
        Instant staleThreshold = Instant.now()
                .minus(workerProperties.job().staleTimeoutMinutes(), ChronoUnit.MINUTES);

        int recovered = asyncJobRepository.resetStaleJobs(staleThreshold);

        if (recovered > 0) {
            log.warn("Recovered {} stale jobs", recovered);
            recoveredCounter.increment(recovered);
        }
        return recovered;
    }
}
