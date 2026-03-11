package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.DTO.BlogCreateDTO;
import com.veterinaire.formulaireveterinaire.DTO.BlogUpdateDTO;
import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import com.veterinaire.formulaireveterinaire.entity.Blog;
import com.veterinaire.formulaireveterinaire.service.BlogService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {


    private final BlogService blogService;

    @Value("${app.upload.pdf-directory:./uploads/pdfs}")
    private String uploadBaseDirectory;

    @PostMapping(value = "/add",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Blog> createBlog(
            @RequestPart("data") BlogCreateDTO dto,
            @RequestPart("pdf") MultipartFile pdf) {

        log.info("Début création blog - Title: {}, Type: {}, File size: {}",
                dto.getTitle(), dto.getType(), pdf.getSize());

        try {
            Blog saved = blogService.createBlog(dto, pdf);
            log.info("Blog créé avec succès - ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception e) {  // Attrape TOUT
            log.error("Erreur critique lors de la création du blog", e);  // ← affiche la stacktrace complète
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);  // ou un message : .body("Erreur serveur : " + e.getMessage())
        }
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Blog> updateBlog(
            @PathVariable Long id,
            @RequestPart(value = "data", required = false) BlogUpdateDTO dto,
            @RequestPart(value = "pdf", required = false) MultipartFile pdf) {

        try {
            // Si aucun data ni pdf → erreur
            if (dto == null && pdf == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // dto peut être null → on passe null au service
            Blog updated = blogService.updateBlog(id, dto, pdf);
            return ResponseEntity.ok(updated);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IOException e) {
            log.error("Erreur lors de l'update du blog {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long id) {
        try {
            blogService.deleteBlog(id);
            return ResponseEntity.noContent().build(); // 204 No Content = succès suppression
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // 404
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du blog {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/all")
    public List<Blog> getAll() {
        return blogService.getAllBlogs();
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Blog>> getByType(@PathVariable String type) {
        BlogType blogType;
        try {
            blogType = BlogType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }

        // Protection uniquement pour VETERINAIRE
        if (blogType == BlogType.VETERINAIRE) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        // OK pour les deux types
        List<Blog> blogs = blogService.getBlogsByType(blogType);
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/pet/{pet}")
    public ResponseEntity<List<Blog>> getByPet(@PathVariable String pet) {
        Pet petType;
        try {
            petType = Pet.valueOf(pet.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        List<Blog> blogs = blogService.getBlogsByPet(petType);
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/pdf/**")
    public ResponseEntity<Resource> servePdf(HttpServletRequest request) throws MalformedURLException {
        // Récupère l'URI complète de la requête
        String requestURI = request.getRequestURI();
        // ex: /api/blogs/pdf/2026/02/6b63dcfc-c3d9-40d8-b9f4-da08cc1418fa.pdf

        // Extrait la partie après /pdf/
        int pdfIndex = requestURI.indexOf("/pdf/");
        if (pdfIndex == -1) {
            log.error("URL invalide pour PDF : {}", requestURI);
            return ResponseEntity.badRequest().build();
        }

        String pathAfterPdf = requestURI.substring(pdfIndex + 5);  // → 2026/02/6b63dcfc-...pdf

        // Chemin complet sur disque
        Path filePath = Paths.get(uploadBaseDirectory, pathAfterPdf).normalize().toAbsolutePath();

        log.info("uploadBaseDirectory = {}", uploadBaseDirectory);
        log.info("Chemin demandé (relatif) = {}", pathAfterPdf);
        log.info("Chemin complet sur disque = {}", filePath);

        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            log.warn("Fichier PDF introuvable : {}", filePath);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
