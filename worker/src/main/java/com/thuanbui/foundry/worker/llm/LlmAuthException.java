package com.thuanbui.foundry.worker.llm;

public class LlmAuthException extends NonRetryableException {
    public LlmAuthException(String message) {
        super(message);
    }
}
