package com.example.otpbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Sends OTP SMS using Fast2SMS (https://www.fast2sms.com/).
 * Sign up, verify your account, and generate an API key from the
 * Fast2SMS dashboard -> Dev API section.
 */
@Service
public class Fast2SmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(Fast2SmsSender.class);
    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";

    @Value("${fast2sms.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void sendOtpSms(String phoneNumber, String otpCode) {
        // Fast2SMS expects a 10-digit Indian number without the +91 prefix
        String localNumber = phoneNumber.replaceFirst("^\\+91", "");

        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMapAdapter body = new MultiValueMapAdapter();
        body.add("route", "otp");
        body.add("variables_values", otpCode);
        body.add("numbers", localNumber);

        HttpEntity<MultiValueMapAdapter> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(FAST2SMS_URL, request, String.class);
            log.info("Fast2SMS response for {}: {}", localNumber, response.getBody());
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}", localNumber, e);
            throw new RuntimeException("Could not send OTP SMS. Please try again.");
        }
    }

    /**
     * Small helper so we don't need an extra Spring import just for
     * form-encoded params. Extends the standard multi-value map.
     */
    private static class MultiValueMapAdapter
            extends org.springframework.util.LinkedMultiValueMap<String, String> {
    }
}
