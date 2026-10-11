package com.thuanbui.foundry.services;

import com.thuanbui.foundry.config.SmsProperties;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsService implements SmsOtpService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);

    private final SmsProperties smsProperties;
    private final MeterRegistry meterRegistry;

    public TwilioSmsService(SmsProperties smsProperties, MeterRegistry meterRegistry) {
        this.smsProperties = smsProperties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        String messageBody = String.format("Mã xác thực của bạn: %s. Hết hạn sau 5 phút.", otp);

        SmsProperties.Twilio twilio = smsProperties.twilio();
        String accountSid = twilio.accountSid();
        String authToken = twilio.authToken();

        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            log.warn("Twilio credentials not configured — logging OTP instead. OTP for {}: {}", phoneNumber, otp);
            return;
        }

        com.twilio.Twilio.init(accountSid, authToken);

        try {
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilio.fromNumber()),
                    messageBody).create();
            meterRegistry.counter("otp_sends_total", "status", "success").increment();
            log.info("Twilio SMS sent to {}: sid={}, status={}", phoneNumber, message.getSid(), message.getStatus());
        } catch (Exception e) {
            meterRegistry.counter("otp_sends_total", "status", "failed").increment();
            log.error("Twilio SMS send failed for {}: {}", phoneNumber, e.getMessage());
            throw e;
        }
    }
}
