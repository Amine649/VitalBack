package com.veterinaire.formulaireveterinaire.DTO;

import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCreateDTO {
    private String title;
    private String description;
    private BlogType type;
    private LocalDateTime createdAt;
    private Pet pet;}