package com.thuanbui.foundry;

import com.thuanbui.foundry.services.SmsOtpService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSmsOtpService implements SmsOtpService {

    private static final Logger log = LoggerFactory.getLogger(TestSmsOtpService.class);

    private final Map<String, String> lastOtpByPhone = new ConcurrentHashMap<>();

    @Override
    public void sendOtp(String phoneNumber, String otp) {
        log.info("TEST MODE: OTP for {} is: {}", phoneNumber, otp);
        lastOtpByPhone.put(phoneNumber, otp);
    }

    public String getLastOtp(String phoneNumber) {
        return lastOtpByPhone.get(phoneNumber);
    }

    public void clear() {
        lastOtpByPhone.clear();
    }
}
