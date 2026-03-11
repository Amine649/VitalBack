package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.DTO.UserDTO;
import com.veterinaire.formulaireveterinaire.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    String registerUser(User user);
    Optional<User> findByEmail(String email);
    List<UserDTO> getAllUsers();
}
