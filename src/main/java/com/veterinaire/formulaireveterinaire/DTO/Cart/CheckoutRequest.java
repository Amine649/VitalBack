package com.veterinaire.formulaireveterinaire.DTO.Cart;

public class CheckoutRequest {
    private String deliveryAddress; // or email

    // Getters and setters
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
}