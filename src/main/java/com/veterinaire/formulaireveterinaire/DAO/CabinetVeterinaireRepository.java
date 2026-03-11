package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.entity.CabinetVeterinaire;
import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CabinetVeterinaireRepository extends JpaRepository<CabinetVeterinaire, Long> {
    Optional<CabinetVeterinaire> findByName(String name);
    Optional<CabinetVeterinaire> findByLatitudeAndLongitudeAndAddress(double latitude, double longitude, String address);
}