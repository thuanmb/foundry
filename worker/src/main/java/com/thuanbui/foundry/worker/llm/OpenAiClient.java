package com.thuanbui.foundry.worker.llm;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.worker.config.OpenAiProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Flux;

@Component
public class OpenAiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final Timer requestTimer;
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter promptTokensCounter;
    private final Counter completionTokensCounter;

    public OpenAiClient(WebClient webClient, MeterRegistry meterRegistry, OpenAiProperties openAiProperties,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.apiKey = openAiProperties.apiKey();
        this.objectMapper = objectMapper;
        this.requestTimer = Timer.builder("worker_llm_request_duration_seconds")
                .register(meterRegistry);
        this.successCounter = Counter.builder("worker_llm_requests_total")
                .tag("status", "success")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("worker_llm_requests_total")
                .tag("status", "error")
                .register(meterRegistry);
        this.promptTokensCounter = Counter.builder("worker_llm_tokens_total")
                .tag("type", "prompt")
                .register(meterRegistry);
        this.completionTokensCounter = Counter.builder("worker_llm_tokens_total")
                .tag("type", "completion")
                .register(meterRegistry);
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        Map<String, Object> body = new HashMap<>(Map.of(
                "model", request.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())),
                "max_tokens", request.maxTokens(),
                "temperature", request.temperature()));
        if (request.jsonMode()) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        Instant start = Instant.now();
        Timer.Sample sample = Timer.start();

        try {
            OpenAiChatResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        int status = clientResponse.statusCode().value();
                        return switch (status) {
                            case 429 -> clientResponse.bodyToMono(String.class)
                                    .map(msg -> new LlmRateLimitException("OpenAI rate limited: " + msg));
                            case 401, 403 -> clientResponse.bodyToMono(String.class)
                                    .map(msg -> new LlmAuthException("OpenAI auth error: " + msg));
                            default -> clientResponse.bodyToMono(String.class)
                                    .map(msg -> new LlmServerException("OpenAI client error " + status + ": " + msg));
                        };
                    })
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(msg -> new LlmServerException("OpenAI server error: " + msg)))
                    .bodyToMono(OpenAiChatResponse.class)
                    .timeout(request.timeout())
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LlmResponseParseException("Empty response from OpenAI", null);
            }

            Duration latency = Duration.between(start, Instant.now());
            sample.stop(requestTimer);

            String content = response.choices().get(0).message().content();
            int promptTokens = response.usage() != null ? response.usage().prompt_tokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completion_tokens() : 0;

            successCounter.increment();
            promptTokensCounter.increment(promptTokens);
            completionTokensCounter.increment(completionTokens);

            log.info("OpenAI chat completed: {} prompt tokens, {} completion tokens, latency {}ms",
                    promptTokens, completionTokens, latency.toMillis());

            return new LlmResponse(content, promptTokens, completionTokens, latency);

        } catch (LlmRateLimitException | LlmAuthException | LlmServerException | LlmResponseParseException e) {
            errorCounter.increment();
            throw e;
        } catch (WebClientResponseException e) {
            errorCounter.increment();
            throw mapWebClientException(e);
        } catch (Exception e) {
            errorCounter.increment();
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                throw new LlmTimeoutException("OpenAI request timed out", e);
            }
            throw new LlmServerException("Unexpected error calling OpenAI: " + e.getMessage());
        }
    }

    @Override
    public Flux<LlmChunk> chatStream(LlmRequest request) {
        var body = Map.of(
                "model", request.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())),
                "max_tokens", request.maxTokens(),
                "temperature", request.temperature(),
                "stream", true,
                "stream_options", Map.of("include_usage", true));

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    int status = clientResponse.statusCode().value();
                    return switch (status) {
                        case 429 -> clientResponse.bodyToMono(String.class)
                                .map(msg -> new LlmRateLimitException("OpenAI rate limited: " + msg));
                        case 401, 403 -> clientResponse.bodyToMono(String.class)
                                .map(msg -> new LlmAuthException("OpenAI auth error: " + msg));
                        default -> clientResponse.bodyToMono(String.class)
                                .map(msg -> new LlmServerException("OpenAI client error " + status + ": " + msg));
                    };
                })
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(msg -> new LlmServerException("OpenAI server error: " + msg)))
                .bodyToFlux(String.class)
                .timeout(request.timeout())
                .mapNotNull(this::parseSseChunk)
                .doOnNext(chunk -> {
                    if (chunk.completionTokens() != null) {
                        completionTokensCounter.increment(chunk.completionTokens());
                    }
                })
                .doOnComplete(successCounter::increment)
                .doOnError(error -> errorCounter.increment())
                .onErrorMap(this::mapStreamError);
    }

    private LlmChunk parseSseChunk(String raw) {
        String data = raw;
        if (data.startsWith("data:")) {
            data = data.substring(5).trim();
        }
        data = data.trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode delta = node.path("choices").path(0).path("delta").path("content");
            String content = delta.isMissingNode() || delta.isNull() ? "" : delta.asText();

            Integer completionTokens = null;
            JsonNode usage = node.get("usage");
            if (usage != null && usage.hasNonNull("completion_tokens")) {
                completionTokens = usage.get("completion_tokens").asInt();
            }

            return new LlmChunk(content, completionTokens);
        } catch (Exception e) {
            throw new LlmResponseParseException("Failed to parse OpenAI stream chunk", e);
        }
    }

    private Throwable mapStreamError(Throwable e) {
        if (e instanceof TimeoutException) {
            return new LlmTimeoutException("OpenAI stream timed out", e);
        }
        if (e instanceof LlmRateLimitException || e instanceof LlmAuthException || e instanceof LlmServerException) {
            return e;
        }
        if (e instanceof WebClientResponseException wce) {
            return mapWebClientException(wce);
        }
        if (e instanceof LlmResponseParseException || e instanceof LlmTimeoutException) {
            return e;
        }
        return new LlmServerException("Unexpected error streaming from OpenAI: " + e.getMessage());
    }

    private RuntimeException mapWebClientException(WebClientResponseException e) {
        return switch (e.getStatusCode().value()) {
            case 429 -> new LlmRateLimitException("OpenAI rate limited");
            case 401, 403 -> new LlmAuthException("OpenAI auth error: " + e.getMessage());
            case 500, 502, 503, 504 -> new LlmServerException("OpenAI server error: " + e.getMessage());
            default -> new LlmServerException("OpenAI error " + e.getStatusCode() + ": " + e.getMessage());
        };
    }
}
