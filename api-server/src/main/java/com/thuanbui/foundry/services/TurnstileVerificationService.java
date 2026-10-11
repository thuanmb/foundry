package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.TurnstileProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TurnstileVerificationService {

    private static final Logger log = LoggerFactory.getLogger(TurnstileVerificationService.class);

    private final TurnstileProperties properties;
    private final HttpClient httpClient;

    public TurnstileVerificationService(TurnstileProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.timeoutSec()))
                .build();
    }

    public boolean verify(String token, String remoteIp) {
        try {
            String body = "secret=" + URLEncoder.encode(properties.secretKey(), StandardCharsets.UTF_8)
                    + "&response=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            if (remoteIp != null && !remoteIp.isBlank()) {
                body += "&remoteip=" + URLEncoder.encode(remoteIp, StandardCharsets.UTF_8);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.verifyUrl()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(properties.timeoutSec()))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Turnstile siteverify returned non-200: {}", response.statusCode());
                return false;
            }

            String responseBody = response.body();
            if (responseBody.contains("\"success\":true")) {
                return true;
            }

            log.warn("Turnstile verification failed. Response: {}", responseBody);
            return false;
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("Turnstile siteverify timed out");
            return false;
        } catch (Exception e) {
            log.error("Turnstile siteverify call failed", e);
            return false;
        }
    }
}
