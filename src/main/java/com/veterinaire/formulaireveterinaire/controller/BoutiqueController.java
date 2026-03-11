package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.entity.Boutique;
import com.veterinaire.formulaireveterinaire.service.BoutiqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boutiques")
public class BoutiqueController {

    private static final Logger logger = LoggerFactory.getLogger(BoutiqueController.class);

    private final BoutiqueService boutiqueService;

    public BoutiqueController(BoutiqueService boutiqueService) {
        this.boutiqueService = boutiqueService;
    }

    @PostMapping("/add")
    public ResponseEntity<?> saveBoutique(@RequestBody Boutique boutique) {
        try {
            Boutique saved = boutiqueService.saveBoutique(boutique);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur validation boutique: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur - add boutique: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de l'ajout de la boutique.");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Boutique>> getAllBoutiques() {
        try {
            return ResponseEntity.ok(boutiqueService.getAllBoutiques());
        } catch (Exception e) {
            logger.error("Erreur récupération boutiques: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBoutique(@PathVariable Long id, @RequestBody Boutique boutique) {
        try {
            Boutique updated = boutiqueService.updateBoutique(id, boutique);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.error("Erreur validation update boutique: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur - update boutique: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de la mise à jour.");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBoutique(@PathVariable Long id) {
        try {
            boutiqueService.deleteBoutique(id);
            return ResponseEntity.ok("Boutique supprimée avec succès.");
        } catch (IllegalArgumentException e) {
            logger.error("Erreur suppression boutique: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur serveur - delete boutique: {}", e.getMessage());
            return ResponseEntity.status(500).body("Erreur serveur lors de la suppression.");
        }
    }
}