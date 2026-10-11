package com.thuanbui.foundry.worker.llm;

public class LlmResponseParseException extends RetryableException {
    public LlmResponseParseException(String message) {
        super(message);
    }

    public LlmResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
