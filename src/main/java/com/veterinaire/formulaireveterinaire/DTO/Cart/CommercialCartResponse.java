package com.veterinaire.formulaireveterinaire.DTO.Cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CommercialCartResponse {
    private Long cartId;
    private String vetMatricule;
    private String vetFullName;
    private String vetEmail;
    private BigDecimal totalAmount;
    private List<CartItemDto> items;
}
