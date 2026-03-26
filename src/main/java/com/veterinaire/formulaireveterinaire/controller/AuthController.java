package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.DTO.LoginDTO;
import com.veterinaire.formulaireveterinaire.service.AuthService;
import com.veterinaire.formulaireveterinaire.service.ForgotPasswordService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ────────────────────────────────────────────────────────────────
    // 1. Login (existing)
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletResponse response) {

        Map<String, Object> responseBody = new HashMap<>();

        try {
            Map<String, Object> authResult = authService.login(loginDTO);

            String token = (String) authResult.get("token");
            boolean isAdmin = (Boolean) authResult.get("isAdmin");

            // Set JWT in httpOnly cookie
            //for test Locally
//            ResponseCookie cookie = ResponseCookie.from("access_token", token)
//                    .httpOnly(true)
//                    .secure(false)          // false for localhost dev, true in prod + HTTPS
//                    .path("/")
//                    .maxAge(7 * 24 * 60 * 60) // 7 days
//                    .sameSite("Lax")        // or "None" + secure=true in prod
//                    .build();

            //for production
            ResponseCookie cookie = ResponseCookie.from("access_token", token)
                    .httpOnly(true)
                    .secure(true)           // REQUIRED for SameSite=None
                    .path("/")
                    .maxAge(2 * 60 * 60) // 2 hours
                    .sameSite("None")       // REQUIRED for cross-site cookies
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            responseBody.put("message", "Login successful");
            responseBody.put("isAdmin", isAdmin);
            responseBody.put("userId", authResult.get("userId"));
            responseBody.put("is_commercial", authResult.get("is_commercial"));

            return ResponseEntity.ok(responseBody);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", "Unauthorized: " + e.getMessage()));
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 2. Logout (existing)
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        return ResponseEntity.ok().build();
    }

    // ────────────────────────────────────────────────────────────────
    // 3. Change password while logged in (existing)
    // ────────────────────────────────────────────────────────────────
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetails userDetails) {    return authService.resetPassword(request, userDetails);
    }



}