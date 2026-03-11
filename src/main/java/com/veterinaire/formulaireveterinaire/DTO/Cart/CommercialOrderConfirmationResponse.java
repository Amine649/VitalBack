package com.veterinaire.formulaireveterinaire.DTO.Cart;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommercialOrderConfirmationResponse {
    private String orderNumber;
    private String vetMatricule;
    private String vetEmail;
    private BigDecimal totalAmount;
}
