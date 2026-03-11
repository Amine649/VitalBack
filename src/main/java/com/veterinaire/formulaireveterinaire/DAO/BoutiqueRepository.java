package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {

    Optional<Boutique> findByName(String name);

    Optional<Boutique> findByLatitudeAndLongitudeAndAddress(
            double latitude,
            double longitude,
            String address);
}
