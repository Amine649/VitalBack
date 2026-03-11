package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserEmail(String email); // Custom query method
}
