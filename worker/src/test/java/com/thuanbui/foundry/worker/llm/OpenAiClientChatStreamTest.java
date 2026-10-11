package com.thuanbui.foundry.worker.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thuanbui.foundry.worker.config.OpenAiProperties;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;

class OpenAiClientChatStreamTest {

    private static final String SSE_BODY = """
            data: {"id":"1","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}

            data: {"id":"2","choices":[{"index":0,"delta":{"content":" world"},"finish_reason":null}]}

            data: {"id":"3","choices":[],"usage":{"prompt_tokens":10,"completion_tokens":2,"total_tokens":12}}

            data: [DONE]

            """;

    private DisposableServer server;
    private OpenAiClient client;

    @BeforeEach
    void setUp() {
        server = HttpServer.create()
                .port(0)
                .handle((req, res) -> res.status(200)
                        .header("Content-Type", "text/event-stream")
                        .sendString(Mono.just(SSE_BODY))
                        .then())
                .bindNow();

        client = buildClient(server.port());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldStreamDeltasAndReportCompletionTokens() {
        List<LlmChunk> chunks = client.chatStream(request()).collectList().block();

        assertEquals(3, chunks.size());
        assertEquals("Hello", chunks.get(0).delta());
        assertEquals(" world", chunks.get(1).delta());
        assertEquals(2, chunks.get(2).completionTokens());
    }

    @Test
    void shouldMapRateLimitError() {
        DisposableServer rateServer = HttpServer.create()
                .port(0)
                .handle((req, res) -> respond(res, 429, "rate limited"))
                .bindNow();
        try {
            OpenAiClient rateClient = buildClient(rateServer.port());
            assertThrows(LlmRateLimitException.class,
                    () -> rateClient.chatStream(request()).collectList().block());
        } finally {
            rateServer.disposeNow();
        }
    }

    @Test
    void shouldMapAuthError() {
        DisposableServer authServer = HttpServer.create()
                .port(0)
                .handle((req, res) -> respond(res, 401, "unauthorized"))
                .bindNow();
        try {
            OpenAiClient authClient = buildClient(authServer.port());
            assertThrows(LlmAuthException.class,
                    () -> authClient.chatStream(request()).collectList().block());
        } finally {
            authServer.disposeNow();
        }
    }

    private Mono<Void> respond(HttpServerResponse res, int status, String body) {
        return res.status(status)
                .header("Content-Type", "application/json")
                .sendString(Mono.just(body))
                .then();
    }

    private LlmRequest request() {
        return new LlmRequest("gpt-4.1-mini", "system", "user", 1000, 0.7, Duration.ofSeconds(10));
    }

    private OpenAiClient buildClient(int port) {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();
        return new OpenAiClient(webClient, new SimpleMeterRegistry(),
                new OpenAiProperties("http://localhost:" + port, "test-key"), new ObjectMapper());
    }
}
