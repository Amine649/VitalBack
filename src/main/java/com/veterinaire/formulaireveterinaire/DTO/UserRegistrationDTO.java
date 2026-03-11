package com.veterinaire.formulaireveterinaire.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRegistrationDTO {
    @NotBlank(message = "Nom est obligatoire")
    private String nom;

    @NotBlank(message = "Prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "Email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String telephone; // Optional

    @NotBlank(message = "Adresse du cabinet est obligatoire")
    private String adresseCabinet;

    @NotBlank(message = "Numéro matricule est obligatoire")
    private String numMatricule;
}
