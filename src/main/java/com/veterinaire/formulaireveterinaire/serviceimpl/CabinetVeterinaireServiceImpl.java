package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.CabinetVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.entity.CabinetVeterinaire;
import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import com.veterinaire.formulaireveterinaire.service.CabinetVeterinaireService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

@Service
public class CabinetVeterinaireServiceImpl implements CabinetVeterinaireService {
    private static final Logger logger = LoggerFactory.getLogger(CabinetVeterinaireServiceImpl.class);

    private final CabinetVeterinaireRepository cabinetVeterinaireRepository;
    private final OurVeterinaireRepository ourVeterinaireRepository;

    public CabinetVeterinaireServiceImpl(CabinetVeterinaireRepository cabinetVeterinaireRepository,
                                        OurVeterinaireRepository ourVeterinaireRepository) {
        this.cabinetVeterinaireRepository = cabinetVeterinaireRepository;
        this.ourVeterinaireRepository = ourVeterinaireRepository;
    }

    @Override
    public CabinetVeterinaire saveCabinet(CabinetVeterinaire cabinet) throws Exception {
        if (cabinet == null) {
            logger.error("Le cabinet vétérinaire fourni est null.");
            throw new IllegalArgumentException("Le cabinet vétérinaire ne peut pas être null.");
        }

        if (cabinet.getName() == null || cabinet.getName().trim().isEmpty()) {
            logger.error("Le nom du cabinet est requis.");
            throw new IllegalArgumentException("Le nom du cabinet est requis.");
        }

        if (cabinet.getAddress() == null || cabinet.getAddress().trim().isEmpty()) {
            logger.error("L'adresse du cabinet est requise.");
            throw new IllegalArgumentException("L'adresse du cabinet est requise.");
        }

        if (cabinet.getMatricule() == null || cabinet.getMatricule().trim().isEmpty()) {
            logger.error("Le matricule du cabinet est requis.");
            throw new IllegalArgumentException("Le matricule du cabinet est requis.");
        }

        // Validate matricule exists in OurVeterinaire
        Optional<OurVeterinaire> vet = ourVeterinaireRepository.findByMatricule(cabinet.getMatricule());
        if (vet.isEmpty()) {
            logger.error("Le matricule {} n'existe pas dans la table OurVeterinaire.", cabinet.getMatricule());
            throw new IllegalArgumentException("Le matricule " + cabinet.getMatricule() + " n'existe pas dans la table OurVeterinaire.");
        }

        // Check for duplicate location (same latitude, longitude, and address)
        Optional<CabinetVeterinaire> existingByLocation = cabinetVeterinaireRepository
                .findByLatitudeAndLongitudeAndAddress(cabinet.getLatitude(), cabinet.getLongitude(), cabinet.getAddress());
        if (existingByLocation.isPresent() && !existingByLocation.get().getName().equals(cabinet.getName())) {
            logger.error("Un cabinet vétérinaire existe déjà à cette adresse et localisation (latitude: {}, longitude: {}).",
                    cabinet.getLatitude(), cabinet.getLongitude());
            throw new IllegalArgumentException("Un cabinet vétérinaire existe déjà à cette adresse et localisation.");
        }

        // Check if a cabinet with the same name exists
        Optional<CabinetVeterinaire> existingByName = cabinetVeterinaireRepository.findByName(cabinet.getName());
        if (existingByName.isPresent()) {
            // Update existing cabinet
            CabinetVeterinaire existing = existingByName.get();
            existing.setName(cabinet.getName());
            existing.setAddress(cabinet.getAddress());
            existing.setCity(cabinet.getCity());
            existing.setPhone(cabinet.getPhone());
            existing.setLatitude(cabinet.getLatitude());
            existing.setLongitude(cabinet.getLongitude());
            existing.setFeatured(cabinet.isFeatured());
            existing.setType(cabinet.getType());
            existing.setMatricule(cabinet.getMatricule());
            logger.info("Mise à jour du cabinet vétérinaire: {}", cabinet.getName());
            return cabinetVeterinaireRepository.save(existing);
        } else {
            // Create new cabinet
            logger.info("Création d'un nouveau cabinet vétérinaire: {}", cabinet.getName());
            return cabinetVeterinaireRepository.save(cabinet);
        }
    }

    @Override
    public CabinetVeterinaire updateCabinet(Long id, CabinetVeterinaire cabinet) throws Exception {
        if (id == null) {
            logger.error("L'ID du cabinet est null.");
            throw new IllegalArgumentException("L'ID du cabinet est requis.");
        }

        if (cabinet == null) {
            logger.error("Le cabinet vétérinaire fourni est null.");
            throw new IllegalArgumentException("Le cabinet vétérinaire ne peut pas être null.");
        }

        if (cabinet.getName() == null || cabinet.getName().trim().isEmpty()) {
            logger.error("Le nom du cabinet est requis.");
            throw new IllegalArgumentException("Le nom du cabinet est requis.");
        }

        if (cabinet.getAddress() == null || cabinet.getAddress().trim().isEmpty()) {
            logger.error("L'adresse du cabinet est requise.");
            throw new IllegalArgumentException("L'adresse du cabinet est requise.");
        }

        if (cabinet.getMatricule() == null || cabinet.getMatricule().trim().isEmpty()) {
            logger.error("Le matricule du cabinet est requis.");
            throw new IllegalArgumentException("Le matricule du cabinet est requis.");
        }

        // Check if cabinet exists
        Optional<CabinetVeterinaire> existing = cabinetVeterinaireRepository.findById(id);
        if (existing.isEmpty()) {
            logger.error("Aucun cabinet vétérinaire trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Aucun cabinet vétérinaire trouvé avec l'ID: " + id);
        }

        // Validate matricule exists in OurVeterinaire
        Optional<OurVeterinaire> vet = ourVeterinaireRepository.findByMatricule(cabinet.getMatricule());
        if (vet.isEmpty()) {
            logger.error("Le matricule {} n'existe pas dans la table OurVeterinaire.", cabinet.getMatricule());
            throw new IllegalArgumentException("Le matricule " + cabinet.getMatricule() + " n'existe pas dans la table OurVeterinaire.");
        }

        // Check for duplicate location (excluding the current cabinet)
        Optional<CabinetVeterinaire> existingByLocation = cabinetVeterinaireRepository
                .findByLatitudeAndLongitudeAndAddress(cabinet.getLatitude(), cabinet.getLongitude(), cabinet.getAddress());
        if (existingByLocation.isPresent() && !existingByLocation.get().getId().equals(id)) {
            logger.error("Un autre cabinet vétérinaire existe déjà à cette adresse et localisation (latitude: {}, longitude: {}).",
                    cabinet.getLatitude(), cabinet.getLongitude());
            throw new IllegalArgumentException("Un autre cabinet vétérinaire existe déjà à cette adresse et localisation.");
        }

        // Update cabinet
        CabinetVeterinaire toUpdate = existing.get();
        toUpdate.setName(cabinet.getName());
        toUpdate.setAddress(cabinet.getAddress());
        toUpdate.setCity(cabinet.getCity());
        toUpdate.setPhone(cabinet.getPhone());
        toUpdate.setLatitude(cabinet.getLatitude());
        toUpdate.setLongitude(cabinet.getLongitude());
        toUpdate.setFeatured(cabinet.isFeatured());
        toUpdate.setType(cabinet.getType());
        toUpdate.setMatricule(cabinet.getMatricule());
        logger.info("Mise à jour du cabinet vétérinaire avec ID: {}", id);
        return cabinetVeterinaireRepository.save(toUpdate);
    }

    @Override
    public void deleteCabinet(Long id) throws Exception {
        if (id == null) {
            logger.error("L'ID du cabinet est null.");
            throw new IllegalArgumentException("L'ID du cabinet est requis.");
        }

        if (!cabinetVeterinaireRepository.existsById(id)) {
            logger.error("Aucun cabinet vétérinaire trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Aucun cabinet vétérinaire trouvé avec l'ID: " + id);
        }

        cabinetVeterinaireRepository.deleteById(id);
        logger.info("Cabinet vétérinaire avec l'ID {} supprimé.", id);
    }

    @Override
    public List<CabinetVeterinaire> getAllCabinets() {
        logger.info("Récupération de tous les cabinets vétérinaires.");
        return cabinetVeterinaireRepository.findAll();
    }
}
