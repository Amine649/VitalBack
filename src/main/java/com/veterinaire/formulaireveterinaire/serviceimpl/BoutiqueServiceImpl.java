package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.BoutiqueRepository;
import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.entity.Boutique;
import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import com.veterinaire.formulaireveterinaire.service.BoutiqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BoutiqueServiceImpl implements BoutiqueService {

    private static final Logger logger = LoggerFactory.getLogger(BoutiqueServiceImpl.class);

    private final BoutiqueRepository boutiqueRepository;
    private final OurVeterinaireRepository ourVeterinaireRepository;

    public BoutiqueServiceImpl(BoutiqueRepository boutiqueRepository,
                               OurVeterinaireRepository ourVeterinaireRepository) {
        this.boutiqueRepository = boutiqueRepository;
        this.ourVeterinaireRepository = ourVeterinaireRepository;
    }

    @Override
    public Boutique saveBoutique(Boutique boutique) throws Exception {
        if (boutique == null) {
            logger.error("La boutique fournie est null.");
            throw new IllegalArgumentException("La boutique ne peut pas être null.");
        }

        if (boutique.getName() == null || boutique.getName().trim().isEmpty()) {
            logger.error("Le nom de la boutique est requis.");
            throw new IllegalArgumentException("Le nom de la boutique est requis.");
        }

        if (boutique.getAddress() == null || boutique.getAddress().trim().isEmpty()) {
            logger.error("L'adresse de la boutique est requise.");
            throw new IllegalArgumentException("L'adresse de la boutique est requise.");
        }

        if (boutique.getMatricule() == null || boutique.getMatricule().trim().isEmpty()) {
            logger.error("Le matricule de la boutique est requis.");
            throw new IllegalArgumentException("Le matricule de la boutique est requis.");
        }

        // Validate matricule exists in OurVeterinaire
        Optional<OurVeterinaire> vet = ourVeterinaireRepository.findByMatricule(boutique.getMatricule());
        if (vet.isEmpty()) {
            logger.error("Le matricule {} n'existe pas dans la table OurVeterinaire.", boutique.getMatricule());
            throw new IllegalArgumentException("Le matricule " + boutique.getMatricule() + " n'existe pas dans la table OurVeterinaire.");
        }

        // Check for duplicate location
        Optional<Boutique> existingByLocation = boutiqueRepository
                .findByLatitudeAndLongitudeAndAddress(
                        boutique.getLatitude(),
                        boutique.getLongitude(),
                        boutique.getAddress());
        if (existingByLocation.isPresent() && !existingByLocation.get().getName().equals(boutique.getName())) {
            logger.error("Une boutique existe déjà à cette adresse et localisation (lat: {}, lon: {}).",
                    boutique.getLatitude(), boutique.getLongitude());
            throw new IllegalArgumentException("Une boutique existe déjà à cette adresse et localisation.");
        }

        // Check if a boutique with the same name exists → update or create
        Optional<Boutique> existingByName = boutiqueRepository.findByName(boutique.getName());
        if (existingByName.isPresent()) {
            Boutique existing = existingByName.get();
            existing.setName(boutique.getName());
            existing.setAddress(boutique.getAddress());
            existing.setCity(boutique.getCity());
            existing.setPhone(boutique.getPhone());
            existing.setLatitude(boutique.getLatitude());
            existing.setLongitude(boutique.getLongitude());
            existing.setFeatured(boutique.isFeatured());
            existing.setType(boutique.getType());
            existing.setMatricule(boutique.getMatricule());
            logger.info("Mise à jour de la boutique existante: {}", boutique.getName());
            return boutiqueRepository.save(existing);
        } else {
            logger.info("Création d'une nouvelle boutique: {}", boutique.getName());
            return boutiqueRepository.save(boutique);
        }
    }

    @Override
    public Boutique updateBoutique(Long id, Boutique boutique) throws Exception {
        if (id == null) {
            logger.error("L'ID de la boutique est null.");
            throw new IllegalArgumentException("L'ID de la boutique est requis.");
        }

        if (boutique == null) {
            logger.error("La boutique fournie est null.");
            throw new IllegalArgumentException("La boutique ne peut pas être null.");
        }

        // Same validations as save
        if (boutique.getName() == null || boutique.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la boutique est requis.");
        }
        if (boutique.getAddress() == null || boutique.getAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("L'adresse de la boutique est requise.");
        }
        if (boutique.getMatricule() == null || boutique.getMatricule().trim().isEmpty()) {
            throw new IllegalArgumentException("Le matricule de la boutique est requis.");
        }

        Optional<Boutique> existingOpt = boutiqueRepository.findById(id);
        if (existingOpt.isEmpty()) {
            logger.error("Aucune boutique trouvée avec l'ID: {}", id);
            throw new IllegalArgumentException("Aucune boutique trouvée avec l'ID: " + id);
        }

        // Validate matricule
        Optional<OurVeterinaire> vet = ourVeterinaireRepository.findByMatricule(boutique.getMatricule());
        if (vet.isEmpty()) {
            throw new IllegalArgumentException("Le matricule " + boutique.getMatricule() + " n'existe pas.");
        }

        // Check duplicate location (exclude self)
        Optional<Boutique> dupLocation = boutiqueRepository
                .findByLatitudeAndLongitudeAndAddress(
                        boutique.getLatitude(),
                        boutique.getLongitude(),
                        boutique.getAddress());
        if (dupLocation.isPresent() && !dupLocation.get().getId().equals(id)) {
            throw new IllegalArgumentException("Une autre boutique existe déjà à cette localisation.");
        }

        Boutique toUpdate = existingOpt.get();
        toUpdate.setName(boutique.getName());
        toUpdate.setAddress(boutique.getAddress());
        toUpdate.setCity(boutique.getCity());
        toUpdate.setPhone(boutique.getPhone());
        toUpdate.setLatitude(boutique.getLatitude());
        toUpdate.setLongitude(boutique.getLongitude());
        toUpdate.setFeatured(boutique.isFeatured());
        toUpdate.setType(boutique.getType());
        toUpdate.setMatricule(boutique.getMatricule());

        logger.info("Mise à jour de la boutique ID: {}", id);
        return boutiqueRepository.save(toUpdate);
    }

    @Override
    public void deleteBoutique(Long id) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de la boutique est requis.");
        }

        if (!boutiqueRepository.existsById(id)) {
            throw new IllegalArgumentException("Aucune boutique trouvée avec l'ID: " + id);
        }

        boutiqueRepository.deleteById(id);
        logger.info("Boutique supprimée – ID: {}", id);
    }

    @Override
    public List<Boutique> getAllBoutiques() {
        logger.info("Récupération de toutes les boutiques.");
        return boutiqueRepository.findAll();
    }
}
