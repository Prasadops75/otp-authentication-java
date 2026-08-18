package com.example.otpbackend.service;

/**
 * Abstraction over "some SMS provider". To switch from Fast2SMS to Twilio,
 * MSG91, or anything else, just write a new class that implements this
 * interface and mark it @Primary (or remove @Service from the old one).
 */
public interface SmsSender {
    void sendOtpSms(String phoneNumber, String otpCode);
}
