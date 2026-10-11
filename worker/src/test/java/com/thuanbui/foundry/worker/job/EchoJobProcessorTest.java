package com.thuanbui.foundry.worker.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.thuanbui.foundry.common.model.entity.AsyncJob;
import com.thuanbui.foundry.common.model.enums.JobType;
import com.thuanbui.foundry.worker.config.OpenAiProperties;
import com.thuanbui.foundry.worker.config.WorkerProperties;
import com.thuanbui.foundry.worker.llm.LlmClient;
import com.thuanbui.foundry.worker.llm.LlmRequest;
import com.thuanbui.foundry.worker.llm.LlmResponse;
import com.thuanbui.foundry.worker.llm.PromptTemplateService;

class EchoJobProcessorTest {

    private final LlmClient llmClient = mock(LlmClient.class);
    private final PromptTemplateService promptTemplateService = mock(PromptTemplateService.class);
    private final GracefulShutdownHandler shutdownHandler = mock(GracefulShutdownHandler.class);

    @Test
    void shouldSupportEchoType() {
        assertThat(processor(false, "key").supportedType()).isEqualTo(JobType.ECHO);
    }

    @Test
    void shouldReturnEchoResultWithoutDelay() {
        Instant before = Instant.now();

        Map<String, Object> result = processor(false, "key").process(job(Map.of("message", "Xin chào", "delaySec", 0)));

        assertThat(result).containsOnlyKeys("echo", "length", "processedAt");
        assertThat(result).containsEntry("echo", "Xin chào").containsEntry("length", 8);
        Instant processedAt = Instant.parse((String) result.get("processedAt"));
        assertThat(processedAt).isBetween(before, Instant.now());
        verify(llmClient, never()).chat(any());
    }

    @Test
    void shouldTreatMissingDelayAsZero() {
        Map<String, Object> result = processor(false, "key").process(job(Map.of("message", "hi")));

        assertThat(result).containsEntry("echo", "hi");
    }

    @Test
    void shouldSleepForDelaySec() {
        long start = System.nanoTime();

        processor(false, "key").process(job(Map.of("message", "hi", "delaySec", 1)));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(950));
    }

    @Test
    void shouldAbortWhenReleasedByShutdown() {
        when(shutdownHandler.isCurrentJobReleased()).thenReturn(true);

        assertThatThrownBy(() -> processor(false, "key").process(job(Map.of("message", "hi", "delaySec", 5))))
                .isInstanceOf(JobInterruptedException.class);
    }

    @Test
    void shouldRejectMissingMessage() {
        assertThatThrownBy(() -> processor(false, "key").process(job(Map.of("delaySec", 0))))
                .isInstanceOf(PermanentJobException.class);
    }

    @Test
    void shouldRejectOutOfRangeDelay() {
        assertThatThrownBy(() -> processor(false, "key").process(job(Map.of("message", "hi", "delaySec", 31))))
                .isInstanceOf(PermanentJobException.class);
        assertThatThrownBy(() -> processor(false, "key").process(job(Map.of("message", "hi", "delaySec", -1))))
                .isInstanceOf(PermanentJobException.class);
        assertThatThrownBy(() -> processor(false, "key").process(job(Map.of("message", "hi", "delaySec", "2"))))
                .isInstanceOf(PermanentJobException.class);
    }

    @Test
    void shouldAddLlmReplyWhenEnabled() {
        when(promptTemplateService.getTemplate(EchoJobProcessor.SYSTEM_PROMPT)).thenReturn("system");
        when(promptTemplateService.render(eq(EchoJobProcessor.USER_PROMPT), any())).thenReturn("user: hi");
        when(llmClient.chat(any())).thenReturn(new LlmResponse("Hello back!", 5, 3, Duration.ofMillis(10)));

        Map<String, Object> result = processor(true, "key").process(job(Map.of("message", "hi")));

        assertThat(result).containsEntry("llmReply", "Hello back!").containsEntry("echo", "hi");
        ArgumentCaptor<LlmRequest> captor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).chat(captor.capture());
        assertThat(captor.getValue().model()).isEqualTo("gpt-4.1-mini");
        assertThat(captor.getValue().systemPrompt()).isEqualTo("system");
        assertThat(captor.getValue().userPrompt()).isEqualTo("user: hi");
        verify(promptTemplateService).render(EchoJobProcessor.USER_PROMPT, Map.of("message", "hi"));
    }

    @Test
    void shouldSkipLlmWhenApiKeyMissing() {
        Map<String, Object> result = processor(true, "").process(job(Map.of("message", "hi")));

        assertThat(result).doesNotContainKey("llmReply");
        verify(llmClient, never()).chat(any());
    }

    private EchoJobProcessor processor(boolean llmEnabled, String apiKey) {
        WorkerProperties properties = new WorkerProperties(
                new WorkerProperties.Job(3, 30, 5, 60, 10),
                new WorkerProperties.Listen("new_job", "job_completed", 30),
                new WorkerProperties.Llm("gpt-4.1-mini", 60, 500, 0.7),
                new WorkerProperties.Echo(llmEnabled, 30));
        return new EchoJobProcessor(properties, new OpenAiProperties("https://api.openai.com/v1", apiKey),
                llmClient, promptTemplateService, shutdownHandler);
    }

    private AsyncJob job(Map<String, Object> params) {
        AsyncJob job = new AsyncJob();
        job.setId(UUID.randomUUID());
        job.setJobType(JobType.ECHO);
        job.setOwnerKey("anon:test");
        job.setParametersHash("hash");
        job.setParameters(new HashMap<>(params));
        return job;
    }
}
