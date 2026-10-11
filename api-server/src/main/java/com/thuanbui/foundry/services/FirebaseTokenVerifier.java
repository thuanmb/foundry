package com.thuanbui.foundry.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenVerifier.class);
    private static final String FIREBASE_LOOKUP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:lookup";

    private final String webApiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FirebaseTokenVerifier(@Value("${firebase.web-api-key}") String webApiKey) {
        this.webApiKey = webApiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String verifyAndGetPhoneNumber(String idToken) {
        if (webApiKey == null || webApiKey.isBlank()) {
            throw new IllegalStateException("FIREBASE_WEB_API_KEY is not configured");
        }

        try {
            String url = FIREBASE_LOOKUP_URL + "?key=" + webApiKey;

            Map<String, String> body = Map.of("idToken", idToken);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Firebase token verification failed: status={}, body={}", response.statusCode(),
                        response.body());
                throw new FirebaseTokenInvalidException("Firebase token verification failed");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode users = root.get("users");
            if (users == null || !users.isArray() || users.size() == 0) {
                throw new FirebaseTokenInvalidException("No user found for the given token");
            }

            JsonNode user = users.get(0);
            String phoneNumber = user.has("phoneNumber") ? user.get("phoneNumber").asText() : null;
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throw new FirebaseTokenInvalidException("Phone number not found in Firebase token");
            }

            log.info("Firebase token verified for phone: {}", phoneNumber);
            return phoneNumber;

        } catch (FirebaseTokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to verify Firebase token: {}", e.getMessage());
            throw new FirebaseTokenInvalidException("Failed to verify Firebase token");
        }
    }

    public static class FirebaseTokenInvalidException extends RuntimeException {
        public FirebaseTokenInvalidException(String message) {
            super(message);
        }
    }
}
