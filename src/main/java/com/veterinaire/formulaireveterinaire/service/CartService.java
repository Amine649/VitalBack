package com.veterinaire.formulaireveterinaire.service;


import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemDto;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemRequest;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartResponse;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CheckoutRequest;


public interface CartService {
    CartResponse getCart(Long userId);
    CartItemDto addItem(Long userId, CartItemRequest req);
    CartItemDto updateItem(Long userId, Long itemId, CartItemRequest req);    void removeItem(Long userId, Long itemId);
    void clearCart(Long userId);

    public String checkout(Long userId, CheckoutRequest request);

}
