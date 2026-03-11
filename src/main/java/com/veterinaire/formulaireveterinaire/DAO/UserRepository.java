package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByStatus(SubscriptionStatus status);
    boolean existsByEmail(String email);
    Optional<User> findByNumMatricule(String numMatricule);
    Optional<User> findByEmailAndPasswordResetOtpAndPasswordResetOtpExpiryAfterAndPasswordResetOtpUsedFalse(
            String email,
            String otp,
            LocalDateTime now
    );

}
