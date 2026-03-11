package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.entity.CabinetVeterinaire;
import com.veterinaire.formulaireveterinaire.service.CabinetVeterinaireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/cabinets")
public class CabinetVeterinaireController {
    private static final Logger logger = LoggerFactory.getLogger(CabinetVeterinaireController.class);

    private final CabinetVeterinaireService cabinetVeterinaireService;

    public CabinetVeterinaireController(CabinetVeterinaireService cabinetVeterinaireService) {
        this.cabinetVeterinaireService = cabinetVeterinaireService;
    }

    @PostMapping("add")
    public ResponseEntity<?> saveCabinet(@RequestBody CabinetVeterinaire cabinet) {
        try {
            CabinetVeterinaire savedCabinet = cabinetVeterinaireService.saveCabinet(cabinet);
            return ResponseEntity.ok(savedCabinet);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de l'enregistrement du cabinet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur lors de l'enregistrement du cabinet: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de l'enregistrement du cabinet.");
        }
    }

    @GetMapping("all")
    public ResponseEntity<List<CabinetVeterinaire>> getAllCabinets() {
        try {
            List<CabinetVeterinaire> cabinets = cabinetVeterinaireService.getAllCabinets();
            return ResponseEntity.ok(cabinets);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des cabinets: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCabinet(@PathVariable Long id, @RequestBody CabinetVeterinaire cabinet) {
        try {
            CabinetVeterinaire updatedCabinet = cabinetVeterinaireService.updateCabinet(id, cabinet);
            return ResponseEntity.ok(updatedCabinet);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la mise à jour du cabinet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur lors de la mise à jour du cabinet: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de la mise à jour du cabinet.");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCabinet(@PathVariable Long id) {
        try {
            cabinetVeterinaireService.deleteCabinet(id);
            return ResponseEntity.ok("Cabinet vétérinaire supprimé avec succès.");
        } catch (IllegalArgumentException e) {
            logger.error("Erreur lors de la suppression du cabinet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur lors de la suppression du cabinet: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de la suppression du cabinet.");
        }
    }


}