package com.thuanbui.foundry.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${sms.provider:}' != 'twilio'")
public class NoOpSmsService implements SmsOtpService {

    private static final Logger log = LoggerFactory.getLogger(NoOpSmsService.class);

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        log.info("DEV MODE: OTP for {} is: {}", phoneNumber, otp);
    }
}
