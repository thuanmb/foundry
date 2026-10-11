package com.thuanbui.foundry.worker.llm;

public class LlmRateLimitException extends RetryableException {
    public LlmRateLimitException(String message) {
        super(message);
    }
}
