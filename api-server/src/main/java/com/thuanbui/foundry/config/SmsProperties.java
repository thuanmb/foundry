package com.thuanbui.foundry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "sms")
@Validated
public record SmsProperties(
        String provider,
        @Valid Twilio twilio) {
    public record Twilio(
            String accountSid,
            String authToken,
            String fromNumber) {
    }
}
