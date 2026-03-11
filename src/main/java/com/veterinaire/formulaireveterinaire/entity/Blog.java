package com.veterinaire.formulaireveterinaire.entity;

import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BlogType type;

    @Column(name = "pdf_filename", nullable = false)
    private String pdfFilename;

    @Column(name = "pdf_relative_path")
    private String pdfRelativePath;         // ex: /uploads/pdfs/2026/02/uuid.pdf

    private Long fileSize;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)   // or nullable = true if optional
    private Pet pet;
}
