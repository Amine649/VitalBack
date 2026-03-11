package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.DTO.UserDTO;
import com.veterinaire.formulaireveterinaire.DTO.UserRegistrationDTO;
import com.veterinaire.formulaireveterinaire.entity.User;
import com.veterinaire.formulaireveterinaire.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        try {
            User user = new User();
            user.setNom(registrationDTO.getNom());
            user.setPrenom(registrationDTO.getPrenom());
            user.setEmail(registrationDTO.getEmail());
            user.setTelephone(registrationDTO.getTelephone());
            user.setAdresseCabinet(registrationDTO.getAdresseCabinet());
            user.setNumMatricule(registrationDTO.getNumMatricule());

            String response = userService.registerUser(user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}