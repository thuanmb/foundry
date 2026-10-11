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
public class IosAppAttestService {

    private static final Logger log = LoggerFactory.getLogger(IosAppAttestService.class);
    private static final String DEV_URL = "https://api.development.devicecheck.apple.com/v1/validateDevice";
    private static final String PROD_URL = "https://api.devicecheck.apple.com/v1/validateDevice";

    private final AttestationProperties properties;
    private final HttpClient httpClient;

    public IosAppAttestService(AttestationProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public boolean verify(String attestationToken) {
        try {
            String verifyUrl = "production".equalsIgnoreCase(properties.iosEnvironment()) ? PROD_URL : DEV_URL;

            String body = String.format("{\"device_token\":\"%s\"}", attestationToken);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(verifyUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.debug("iOS attestation verification successful");
                return true;
            }

            log.warn("iOS attestation verification failed: status={}, body={}", response.statusCode(), response.body());
            return false;
        } catch (java.net.http.HttpTimeoutException e) {
            log.error("iOS attestation verification timed out");
            return false;
        } catch (Exception e) {
            log.error("iOS attestation verification failed", e);
            return false;
        }
    }
}
