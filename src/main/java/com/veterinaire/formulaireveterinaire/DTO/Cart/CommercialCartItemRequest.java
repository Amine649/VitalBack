package com.veterinaire.formulaireveterinaire.DTO.Cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommercialCartItemRequest {
    @NotNull
    private Long productId;

    @Min(1)
    private Integer quantity;

    private Long variantId;
}
