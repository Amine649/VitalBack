package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.entity.OurVeterinaire;
import com.veterinaire.formulaireveterinaire.service.OurVeterinaireService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/veterinaires")
public class OurVeterinaireController {
    private final OurVeterinaireService ourVeterinaireService;

    public OurVeterinaireController(OurVeterinaireService ourVeterinaireService) {
        this.ourVeterinaireService = ourVeterinaireService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<OurVeterinaire>> getAllVeterinaires() {
        List<OurVeterinaire> veterinaires = ourVeterinaireService.getAllVeterinaires();
        return ResponseEntity.ok(veterinaires);
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            ourVeterinaireService.uploadExcel(file);
            return ResponseEntity.ok("Fichier Excel traité avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }
}
