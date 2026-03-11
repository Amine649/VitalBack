package com.veterinaire.formulaireveterinaire.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "cabinet_veterinaires")
@Data
@NoArgsConstructor
public class CabinetVeterinaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = true)
    private String City;

    @Column(nullable = true)
    private String phone;

    @Column(nullable = true)
    private double  latitude;

    @Column(nullable = true)
    private double  longitude;

    @Column(nullable = false)
    private boolean isFeatured = false;

    @Column(nullable = false)
    private String type = "Cabinet";

    @Column(nullable = false)
    private String matricule ;
}
