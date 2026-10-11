package com.thuanbui.foundry.worker.llm;

import java.util.List;

record OpenAiChatResponse(
    List<Choice> choices,
    Usage usage,
    String error
) {
    record Choice(Message message) {}
    record Message(String content) {}
    record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}
}