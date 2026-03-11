package com.veterinaire.formulaireveterinaire.DTO;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String adresseCabinet;
    private String numMatricule;
    private String status;
    private SubscriptionDTO subscription;
}