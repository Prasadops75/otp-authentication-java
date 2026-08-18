package com.example.otpbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    // In-memory store: phone -> OtpEntry. Fine for a demo / single-instance
    // server. For production with multiple server instances, replace this
    // with Redis (with a TTL) so all instances share the same OTP state.
    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALID_MILLIS = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_ATTEMPTS = 5;                  // guard against brute force

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private SmsSender smsSender;

    /**
     * Generates a fresh OTP, stores it with an expiry, and sends it via SMS.
     */
    public void generateAndSendOtp(String phone) {
        String otp = generateNumericOtp();
        otpStore.put(phone, new OtpEntry(otp, Instant.now().toEpochMilli() + OTP_VALID_MILLIS));
        smsSender.sendOtpSms(phone, otp);
    }

    /**
     * Verifies a code against what's stored for that phone number.
     * Returns true only if it matches AND hasn't expired AND attempts
     * haven't been exhausted. Successful verification clears the entry
     * so it can't be reused (a code is single-use).
     */
    public boolean verifyOtp(String phone, String code) {
        OtpEntry entry = otpStore.get(phone);

        if (entry == null) {
            return false; // no OTP was ever requested, or it was already used
        }

        if (Instant.now().toEpochMilli() > entry.expiresAtMillis) {
            otpStore.remove(phone);
            return false; // expired
        }

        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(phone);
            return false; // too many wrong tries - force a fresh OTP request
        }

        entry.attempts++;

        if (entry.code.equals(code)) {
            otpStore.remove(phone); // one-time use
            return true;
        }

        return false;
    }

    private String generateNumericOtp() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /** Holds a code, its expiry timestamp, and how many verify attempts have been made. */
    private static class OtpEntry {
        final String code;
        final long expiresAtMillis;
        int attempts = 0;

        OtpEntry(String code, long expiresAtMillis) {
            this.code = code;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
