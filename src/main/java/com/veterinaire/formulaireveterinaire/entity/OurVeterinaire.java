package com.veterinaire.formulaireveterinaire.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "our_veterinaires")
@Data
@NoArgsConstructor
public class OurVeterinaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;


    @Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false, unique = true)   // or nullable = true if optional
    private String email;
}
