package com.thuanbui.foundry.worker.job;

import com.thuanbui.foundry.worker.llm.NonRetryableException;

/**
 * Permanent job failure (e.g. invalid parameters, unknown job type); the job is
 * marked FAILED.
 */
public class PermanentJobException extends NonRetryableException {
    public PermanentJobException(String message) {
        super(message);
    }
}
