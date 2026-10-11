package com.thuanbui.foundry.worker.llm;

import java.time.Duration;

public record LlmResponse(
        String content,
        int promptTokens,
        int completionTokens,
        Duration latency) {
}
