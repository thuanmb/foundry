package com.thuanbui.foundry.worker.job;

import java.util.Map;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobType;

/**
 * Plug-in point for job handlers. Register an implementation as a Spring bean
 * and the
 * {@link JobDispatcher} routes jobs of {@link #supportedType()} to it.
 * <p>
 * Throw a {@code RetryableException} for transient errors (job goes back to
 * PENDING until
 * maxRetries) or a {@code NonRetryableException} for permanent errors (job goes
 * to FAILED).
 */
public interface JobProcessor {

    JobType supportedType();

    /** Returns the result map stored in {@code async_jobs.result}. */
    Map<String, Object> process(AsyncJob job);
}
