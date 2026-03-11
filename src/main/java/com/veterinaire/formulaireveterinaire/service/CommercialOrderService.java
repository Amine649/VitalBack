package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.DTO.Cart.*;
import com.veterinaire.formulaireveterinaire.entity.CartOrder;
import com.veterinaire.formulaireveterinaire.entity.OrderItem;

import java.util.Optional;

public interface CommercialOrderService {

     CartOrder loadOrCreateCommercialCart(String vetMatricule);

    CommercialCartResponse getOrCreateCommercialCart(String vetMatricule);

    CartItemDto addItemToCommercialCart(String vetMatricule, CommercialCartItemRequest request, Long commercialUserId);


    public CartItemDto updateCommercialCartItem(String vetMatricule, Long itemId, CommercialCartItemRequest req, Long commercialUserId);

    void removeCommercialCartItem(String vetMatricule, Long itemId, Long commercialUserId);

    void clearCommercialCart(String vetMatricule, Long commercialUserId);

    CommercialOrderConfirmationResponse confirmCommercialOrder(String vetMatricule, Long commercialUserId);
}