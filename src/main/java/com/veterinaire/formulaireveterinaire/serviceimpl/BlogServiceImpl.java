package com.veterinaire.formulaireveterinaire.serviceimpl;

import com.veterinaire.formulaireveterinaire.DAO.BlogRepository;
import com.veterinaire.formulaireveterinaire.DTO.BlogCreateDTO;
import com.veterinaire.formulaireveterinaire.DTO.BlogUpdateDTO;
import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import com.veterinaire.formulaireveterinaire.entity.Blog;
import com.veterinaire.formulaireveterinaire.service.BlogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;

    @Value("${app.upload.pdf-directory:./uploads/pdfs}")
    private String uploadBaseDirectory;

    @Override
    public Blog createBlog(BlogCreateDTO dto, MultipartFile pdfFile) throws IOException {

        if (dto == null) {
            throw new IllegalArgumentException("DTO ne peut pas être null");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Type du blog requis");
        }
        if (dto.getPet() == null) {                               // ← add
            throw new IllegalArgumentException("Pet type requis (CAT ou DOG)");
        }

        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("Fichier PDF requis et non vide");
        }

        log.info("Upload PDF - Original name: {}, Size: {}", pdfFile.getOriginalFilename(), pdfFile.getSize());

        String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = FilenameUtils.getExtension(pdfFile.getOriginalFilename());
        String uniqueName = UUID.randomUUID() + (extension.isBlank() ? ".pdf" : "." + extension);

        Path subDir = Paths.get(uploadBaseDirectory, yearMonth);
        Path destination = subDir.resolve(uniqueName);

        log.info("Tentative création dossier: {}", subDir);
        Files.createDirectories(subDir);  // ← throw IOException si échec

        log.info("Écriture fichier vers: {}", destination);
        pdfFile.transferTo(destination.toFile());  // ← ici souvent l'IOException

        String relativePath = "/uploads/pdfs/" + yearMonth + "/" + uniqueName;

        Blog blog = Blog.builder()
                .title(dto.getTitle() != null ? dto.getTitle().trim() : "")
                .description(dto.getDescription())
                .type(dto.getType())
                .pet(dto.getPet())
                .pdfFilename(uniqueName)
                .pdfRelativePath(relativePath)
                .fileSize(pdfFile.getSize())
                .createdAt(LocalDateTime.now())
                .build();

        log.info("Sauvegarde en base...");
        return blogRepository.save(blog);
    }

    @Override
    public Blog updateBlog(Long id, BlogUpdateDTO dto, MultipartFile newPdf) throws IOException {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog non trouvé avec ID " + id));

        // Mise à jour des champs texte seulement s'ils sont fournis
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            blog.setTitle(dto.getTitle().trim());
        }

        if (dto.getDescription() != null) {
            blog.setDescription(dto.getDescription());
        }

        if (dto.getType() != null) {
            blog.setType(dto.getType());
        }

        if (dto.getPet() != null) {                    // ← new
            blog.setPet(dto.getPet());
        }

        // Gestion du PDF : si un nouveau fichier est envoyé → remplacer l'ancien
        if (newPdf != null && !newPdf.isEmpty()) {
            if (!"application/pdf".equalsIgnoreCase(newPdf.getContentType())) {
                throw new IllegalArgumentException("Le fichier doit être un PDF");
            }

            // 1. Supprimer l'ancien PDF (si existe)
            if (blog.getPdfRelativePath() != null) {
                String oldFilePathStr = uploadBaseDirectory + blog.getPdfRelativePath().substring(1);
                Path oldFilePath = Paths.get(oldFilePathStr).toAbsolutePath().normalize();
                if (Files.exists(oldFilePath)) {
                    try {
                        Files.delete(oldFilePath);
                        log.info("Ancien PDF supprimé : {}", oldFilePath);
                    } catch (IOException e) {
                        log.warn("Impossible de supprimer l'ancien PDF : {}", e.getMessage());
                        // On continue quand même
                    }
                }
            }

            // 2. Sauvegarder le nouveau PDF
            String yearMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String extension = FilenameUtils.getExtension(newPdf.getOriginalFilename());
            String uniqueName = UUID.randomUUID() + (extension.isBlank() ? ".pdf" : "." + extension);

            Path subDir = Paths.get(uploadBaseDirectory, yearMonth);
            Files.createDirectories(subDir);

            Path destination = subDir.resolve(uniqueName);
            Files.copy(newPdf.getInputStream(), destination);   // ou transferTo

            // 3. Mettre à jour les champs PDF dans l'entité
            String newRelativePath = "/uploads/pdfs/" + yearMonth + "/" + uniqueName;
            blog.setPdfFilename(uniqueName);
            blog.setPdfRelativePath(newRelativePath);
            blog.setFileSize(newPdf.getSize());
        }

        // Mise à jour en base
        return blogRepository.save(blog);
    }

    @Override
    public void deleteBlog(Long id) throws IOException {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Blog non trouvé avec ID " + id));

        // 1. Supprimer le fichier physique
        if (blog.getPdfRelativePath() != null) {
            String relativePath = blog.getPdfRelativePath(); // ex: /uploads/pdfs/2026/02/uuid.pdf
            String filePathStr = uploadBaseDirectory + relativePath.substring(1); // enlève le premier /

            Path filePath = Paths.get(filePathStr).toAbsolutePath().normalize();

            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.info("Fichier PDF supprimé : {}", filePath);
                } catch (IOException e) {
                    log.warn("Impossible de supprimer le fichier PDF : {}. Erreur : {}", filePath, e.getMessage());
                    // On continue quand même (on supprime en base même si fichier reste)
                }
            } else {
                log.warn("Fichier PDF introuvable lors de la suppression : {}", filePath);
            }
        }

        // 2. Supprimer l'entrée en base
        blogRepository.deleteById(id);
        log.info("Blog supprimé (ID {})", id);
    }

    @Override
    public List<Blog> getAllBlogs() {
        return blogRepository.findAll();
    }

    @Override
    public List<Blog> getBlogsByType(BlogType type) {
        return blogRepository.findByType(type);
    }

    @Override
    public List<Blog> getBlogsByPet(Pet pet) {
        return blogRepository.findByPet(pet);
    }

    @Override
    public Blog getBlogById(Long id) {
        return blogRepository.findById(id).orElse(null);
    }
}
