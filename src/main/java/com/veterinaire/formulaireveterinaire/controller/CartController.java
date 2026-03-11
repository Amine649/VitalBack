package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemDto;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemRequest;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartResponse;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CheckoutRequest;
import com.veterinaire.formulaireveterinaire.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestParam Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemDto> addItem(
            @RequestParam Long userId,
            @Valid @RequestBody CartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(userId, req));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItemDto> updateItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @RequestBody CartItemRequest req) {
        CartItemDto dto = cartService.updateItem(userId, itemId, req);
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        cartService.removeItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/checkout")
    public ResponseEntity<String> checkout(
            @RequestParam Long userId,
            @RequestBody CheckoutRequest request) {
        String orderNumber = cartService.checkout(userId, request);
        return ResponseEntity.ok(orderNumber);
    }



}
