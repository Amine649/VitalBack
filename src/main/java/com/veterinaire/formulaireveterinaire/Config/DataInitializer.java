package com.veterinaire.formulaireveterinaire.Config;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionStatus;
import com.veterinaire.formulaireveterinaire.DAO.UserRepository;
import com.veterinaire.formulaireveterinaire.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    CommandLineRunner initAdmin() {
        return args -> {
            if (!userRepository.existsByEmail("admin@example.com")) {
                User admin = new User();
                admin.setNom("Admin");
                admin.setPrenom("User");
                admin.setEmail("admin@example.com");
                admin.setNumMatricule("ADMIN123");
                admin.setAdresseCabinet("Admin Office");
                admin.setPassword("$2a$12$Gc3wvZUBgr5AYKpU2Y7.teXzKKvoAu04LzpDecze8iNQCkhcANy5a"); // mot de passe déjà hashé
                admin.setAdmin(true);
                admin.setCommercial(false);   // 🔹 AJOUT


                admin.setStatus(SubscriptionStatus.ACTIVE);
                userRepository.save(admin);
            }

            if (!userRepository.existsByEmail("commercial@example.com")) {
                User commerciale = new User();
                commerciale.setNom("Commercial");
                commerciale.setPrenom("Equipe");
                commerciale.setEmail("commercial@example.com");
                commerciale.setNumMatricule("COMM2025");
                commerciale.setAdresseCabinet("Commercial Dept");
                commerciale.setPassword("$2a$12$pNBkVVUPVG9Ffi7ydn2hMOzPhEAzjkNo1iDq915jujQ.T4XbiVlX.");  // ← change this in production!
                commerciale.setAdmin(false);   // ← important: not admin
                commerciale.setCommercial(true);   // 🔹 SEUL LUI EST TRUE
                commerciale.setStatus(SubscriptionStatus.ACTIVE);
                userRepository.save(commerciale);
                System.out.println("Commercial user created");
            }
        };
    }
}


