package com.thuanbui.foundry.worker.job;

import com.thuanbui.foundry.worker.llm.RetryableException;

/**
 * Job aborted before finishing (interrupt or worker shutdown); the job is put
 * back to PENDING.
 */
public class JobInterruptedException extends RetryableException {
    public JobInterruptedException(String message) {
        super(message);
    }
}
