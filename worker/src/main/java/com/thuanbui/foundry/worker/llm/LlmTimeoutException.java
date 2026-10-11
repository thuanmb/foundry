package com.thuanbui.foundry.worker.llm;

public class LlmTimeoutException extends RetryableException {
    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
