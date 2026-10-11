package com.thuanbui.foundry.services;

public interface SmsOtpService {
    void sendOtp(String phoneNumber, String otp);
}
