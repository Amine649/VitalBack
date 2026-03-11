package com.veterinaire.formulaireveterinaire.DTO.Cart;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private BigDecimal totalAmount;
    private List<CartItemDto> items;
}