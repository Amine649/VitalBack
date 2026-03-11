package com.veterinaire.formulaireveterinaire.DTO;

import com.veterinaire.formulaireveterinaire.Enums.BlogType;
import com.veterinaire.formulaireveterinaire.Enums.Pet;
import lombok.Data;

@Data
public class BlogUpdateDTO {
    private String title;           // optionnel
    private String description;     // optionnel
    private BlogType type;          // optionnel
    private Pet pet;}
