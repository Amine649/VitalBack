package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.service.ForgotPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;


    // ────────────────────────────────────────────────────────────────
    // 4. Request OTP for forgot password
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/forgot-password-otp")
    public ResponseEntity<String> requestOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email requis");
        }

        String message = forgotPasswordService.requestOtp(email);
        return ResponseEntity.ok(message);
    }

    // ────────────────────────────────────────────────────────────────
    // 5. Verify OTP and reset password
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/reset-password-otp")
    public ResponseEntity<String> resetPasswordWithOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest().body("email, otp et newPassword sont requis");
        }

        String result = forgotPasswordService.verifyOtpAndChangePassword(email, otp, newPassword);
        return ResponseEntity.ok(result);
    }
}
