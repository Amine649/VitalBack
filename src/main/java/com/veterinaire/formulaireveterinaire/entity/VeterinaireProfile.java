package com.veterinaire.formulaireveterinaire.entity;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "veterinaire_profiles")
@Data
@NoArgsConstructor
public class VeterinaireProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column
    private SubscriptionType subscriptionType;

    @Column
    private String imagePath;
}
