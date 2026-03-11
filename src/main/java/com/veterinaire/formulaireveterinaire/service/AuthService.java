package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.DTO.LoginDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginDTO loginDTO);
    ResponseEntity<String> resetPassword(Map<String, String> request, UserDetails userDetails);
}
