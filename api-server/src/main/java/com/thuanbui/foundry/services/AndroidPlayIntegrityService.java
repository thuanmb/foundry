package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.AttestationProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AndroidPlayIntegrityService {

    private static final Logger log = LoggerFactory.getLogger(AndroidPlayIntegrityService.class);
    private static final String PLAY_INTEGRITY_URL = "https://playintegrity.googleapis.com/v1/%s:decodeIntegrityToken";

    private final AttestationProperties properties;
    private final HttpClient httpClient;

    public AndroidPlayIntegrityService(AttestationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean verify(String integrityToken) {
        try {
            String url = String.format(PLAY_INTEGRITY_URL, properties.androidPackageName());

            String body = String.format("{\"integrity_token\":\"%s\"}", integrityToken);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Play Integrity verification failed: status={}", response.statusCode());
                return false;
            }

            String responseBody = response.body();
            // Verify app recognition verdict
            if (!responseBody.contains("\"appRecognitionVerdict\":\"PLAY_RECOGNIZED\"")) {
                log.warn("Play Integrity: app not recognized. Response: {}", responseBody);
                return false;
            }

            // Verify device integrity
            if (!responseBody.contains("\"deviceRecognitionVerdict\":")
                    && !responseBody.contains("MEETS_DEVICE_INTEGRITY")) {
                log.warn("Play Integrity: device integrity not met. Response: {}", responseBody);
                return false;
            }

            log.debug("Play Integrity verification successful");
            return true;
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("Play Integrity verification timed out");
            return false;
        } catch (Exception e) {
            log.error("Play Integrity verification failed", e);
            return false;
        }
    }
}
