package com.thuanbui.foundry.worker.llm;

public record LlmChunk(
        String delta,
        Integer completionTokens) {
}
