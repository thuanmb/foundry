package com.thuanbui.foundry.worker.job;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.worker.config.OpenAiProperties;
import com.thuanbui.foundry.worker.config.WorkerProperties;
import com.thuanbui.foundry.worker.llm.LlmClient;
import com.thuanbui.foundry.worker.llm.LlmRequest;
import com.thuanbui.foundry.worker.llm.LlmResponse;
import com.thuanbui.foundry.worker.llm.PromptTemplateService;

/**
 * Demo job: parameters {@code {message, delaySec?}} → result
 * {@code {echo, length, processedAt, llmReply?}}.
 */
@Component
public class EchoJobProcessor implements JobProcessor {

    static final String SYSTEM_PROMPT = "echo_prompt.system.txt";
    static final String USER_PROMPT = "echo_prompt.user.txt";
    private static final long SLEEP_SLICE_MS = 250;

    private static final Logger log = LoggerFactory.getLogger(EchoJobProcessor.class);

    private final WorkerProperties workerProperties;
    private final OpenAiProperties openAiProperties;
    private final LlmClient llmClient;
    private final PromptTemplateService promptTemplateService;
    private final GracefulShutdownHandler gracefulShutdownHandler;

    public EchoJobProcessor(WorkerProperties workerProperties,
            OpenAiProperties openAiProperties,
            LlmClient llmClient,
            PromptTemplateService promptTemplateService,
            GracefulShutdownHandler gracefulShutdownHandler) {
        this.workerProperties = workerProperties;
        this.openAiProperties = openAiProperties;
        this.llmClient = llmClient;
        this.promptTemplateService = promptTemplateService;
        this.gracefulShutdownHandler = gracefulShutdownHandler;
    }

    @Override
    public JobType supportedType() {
        return JobType.ECHO;
    }

    @Override
    public Map<String, Object> process(AsyncJob job) {
        Map<String, Object> params = job.getParameters() != null ? job.getParameters() : Map.of();
        String message = parseMessage(params.get("message"));
        int delaySec = parseDelaySec(params.get("delaySec"));

        sleep(Duration.ofSeconds(delaySec));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("echo", message);
        result.put("length", message.length());
        result.put("processedAt", Instant.now().toString());

        if (isLlmEnabled()) {
            result.put("llmReply", askLlm(message));
        }
        return result;
    }

    private String parseMessage(Object value) {
        if (!(value instanceof String message) || message.isEmpty()) {
            throw new PermanentJobException("Parameter 'message' must be a non-empty string");
        }
        return message;
    }

    private int parseDelaySec(Object value) {
        if (value == null) {
            return 0;
        }
        if (!(value instanceof Number number) || number.doubleValue() != number.intValue()) {
            throw new PermanentJobException("Parameter 'delaySec' must be an integer");
        }
        int delaySec = number.intValue();
        int max = workerProperties.echo().maxDelaySec();
        if (delaySec < 0 || delaySec > max) {
            throw new PermanentJobException("Parameter 'delaySec' must be between 0 and " + max);
        }
        return delaySec;
    }

    // Sleeps in short slices so a job released by graceful shutdown stops promptly.
    private void sleep(Duration duration) {
        long deadline = System.nanoTime() + duration.toNanos();
        try {
            while (true) {
                if (gracefulShutdownHandler.isCurrentJobReleased()) {
                    throw new JobInterruptedException("Released by worker shutdown");
                }
                long remainingMs = (deadline - System.nanoTime()) / 1_000_000;
                if (remainingMs <= 0) {
                    return;
                }
                Thread.sleep(Math.min(remainingMs, SLEEP_SLICE_MS));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobInterruptedException("Job interrupted");
        }
    }

    private boolean isLlmEnabled() {
        String apiKey = openAiProperties.apiKey();
        return workerProperties.echo().llmEnabled() && apiKey != null && !apiKey.isBlank();
    }

    private String askLlm(String message) {
        WorkerProperties.Llm llm = workerProperties.llm();
        LlmRequest request = new LlmRequest(
                llm.model(),
                promptTemplateService.getTemplate(SYSTEM_PROMPT),
                promptTemplateService.render(USER_PROMPT, Map.of("message", message)),
                llm.maxTokens(),
                llm.temperature(),
                Duration.ofSeconds(llm.timeoutSec()));
        LlmResponse response = llmClient.chat(request);
        log.debug("Echo LLM reply received ({} completion tokens)", response.completionTokens());
        return response.content();
    }
}
