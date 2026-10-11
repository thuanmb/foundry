package com.thuanbui.foundry.worker.llm;

import reactor.core.publisher.Flux;

public interface LlmClient {
    LlmResponse chat(LlmRequest request);

    Flux<LlmChunk> chatStream(LlmRequest request);
}
