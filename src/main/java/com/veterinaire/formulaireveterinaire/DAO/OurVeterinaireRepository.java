package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OurVeterinaireRepository extends JpaRepository<OurVeterinaire, Long> {
    Optional<OurVeterinaire> findByMatricule(String matricule);

}
