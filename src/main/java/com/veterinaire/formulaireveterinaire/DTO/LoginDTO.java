package com.veterinaire.formulaireveterinaire.DTO;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank(message = "L'email est requis") String email,
        @NotBlank(message = "Le mot de passe est requis") String password
) {}
