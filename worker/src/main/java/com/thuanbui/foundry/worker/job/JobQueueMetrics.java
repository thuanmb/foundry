package com.thuanbui.foundry.worker.job;

import com.thuanbui.foundry.common.model.enums.JobStatus;
import com.thuanbui.foundry.common.repository.AsyncJobRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class JobQueueMetrics {

    private final AsyncJobRepository asyncJobRepository;
    private final MeterRegistry meterRegistry;

    public JobQueueMetrics(AsyncJobRepository asyncJobRepository, MeterRegistry meterRegistry) {
        this.asyncJobRepository = asyncJobRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void registerGauge() {
        Gauge.builder("worker_jobs_queue_depth", asyncJobRepository,
                r -> r.countByStatusAndDeletedFalse(JobStatus.PENDING))
                .description("Number of pending jobs in the queue")
                .register(meterRegistry);
    }
}
