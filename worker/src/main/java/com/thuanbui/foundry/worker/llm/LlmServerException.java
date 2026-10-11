package com.thuanbui.foundry.worker.llm;

public class LlmServerException extends RetryableException {
    public LlmServerException(String message) {
        super(message);
    }
}
