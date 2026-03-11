package com.veterinaire.formulaireveterinaire.service;

public interface ForgotPasswordService {
    String requestOtp(String email);
    String verifyOtpAndChangePassword(String email, String otp, String newPassword);
}