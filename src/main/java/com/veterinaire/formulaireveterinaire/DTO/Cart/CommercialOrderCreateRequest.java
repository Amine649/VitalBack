package com.veterinaire.formulaireveterinaire.DTO.Cart;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommercialOrderCreateRequest {
    @NotNull(message = "Matricule du vétérinaire obligatoire")
    private String vetMatricule;   // ← key field

    private List<CommercialCartItemRequest> items = new ArrayList<>();
}
