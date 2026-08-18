package com.example.otpbackend.controller;

import com.example.otpbackend.model.ApiResponse;
import com.example.otpbackend.model.SendOtpRequest;
import com.example.otpbackend.model.VerifyOtpRequest;
import com.example.otpbackend.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    // POST /api/otp/send  { "phone": "+919876543210" }
    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            otpService.generateAndSendOtp(request.getPhone());
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Failed to send OTP: " + e.getMessage()));
        }
    }

    // POST /api/otp/verify  { "phone": "+919876543210", "code": "123456" }
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        boolean isValid = otpService.verifyOtp(request.getPhone(), request.getCode());

        if (isValid) {
            // In a real app: create/find the user record here and issue a
            // session token or JWT to return to the Android app.
            return ResponseEntity.ok(new ApiResponse(true, "OTP verified successfully"));
        } else {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid or expired OTP"));
        }
    }
}
