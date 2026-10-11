package com.thuanbui.foundry.worker.llm;

import java.time.Duration;

public record LlmRequest(
        String model,
        String systemPrompt,
        String userPrompt,
        int maxTokens,
        double temperature,
        Duration timeout,
        boolean jsonMode) {

    public LlmRequest(String model, String systemPrompt, String userPrompt, int maxTokens, double temperature,
            Duration timeout) {
        this(model, systemPrompt, userPrompt, maxTokens, temperature, timeout, false);
    }
}
