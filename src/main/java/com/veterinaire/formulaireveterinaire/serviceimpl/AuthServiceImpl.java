package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.Config.JwtUtil;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.DTO.LoginDTO;

import com.veterinaire.formulaireveterinaire.entity.User;
import com.veterinaire.formulaireveterinaire.service.AuthService;
import com.veterinaire.formulaireveterinaire.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                           UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        try {
            // Authenticate user with email and password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.email(), loginDTO.password())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Load user details
            User user = userService.findByEmail(loginDTO.email())
                    .orElseThrow(() -> new RuntimeException("User not found: " + loginDTO.email()));

            // For non-admin users (isAdmin = false), allow login regardless of status or subscription
            // Admins (isAdmin = true) can always log in
            // No checks for status or subscription expiration for non-admins

            // Generate JWT token
            String token = jwtUtil.generateToken(loginDTO.email(), user.isAdmin());
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);

            response.put("isAdmin", user.isAdmin());

            response.put("userId", user.getId());

            response.put("is_commercial", user.isCommercial());

            return response;
        } catch (AuthenticationException e) {
            throw new RuntimeException("Unauthorized: Bad credentials", e);
        }
    }


    @Override
    public ResponseEntity<String> resetPassword(Map<String, String> request, UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"Invalid or missing authentication\"}");
        }    String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\": \"User not found\"}");
        }

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank() ||
                newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Both current and new passwords are required\"}");
        }

        //  Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"Current password is incorrect\"}");
        }

        //  Save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("{\"message\": \"Mot de passe réinitialisé avec succès\"}");
    }


}
