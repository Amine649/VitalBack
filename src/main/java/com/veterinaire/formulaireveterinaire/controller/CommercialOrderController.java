package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.DAO.OurVeterinaireRepository;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CartItemDto;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialCartItemRequest;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialCartResponse;
import com.veterinaire.formulaireveterinaire.DTO.Cart.CommercialOrderConfirmationResponse;
import com.veterinaire.formulaireveterinaire.service.CommercialOrderService;
import com.veterinaire.formulaireveterinaire.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/commercial/orders")
@RequiredArgsConstructor
public class CommercialOrderController {

    private final CommercialOrderService commercialOrderService;
    private final UserService userService; // or SecurityContext to get current user
    private final OurVeterinaireRepository ourVeterinaireRepository;


    @GetMapping("/check/{matricule}")
    public ResponseEntity<?> checkVeterinaire(@PathVariable String matricule) {

        // 1️⃣ Validation basique
        if (matricule == null || matricule.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Le matricule est requis"
                    ));
        }

        // 2️⃣ Vérification existence
        return ourVeterinaireRepository.findByMatricule(matricule)
                .map(vet -> ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "message", "Vétérinaire trouvé",
                                "matricule", vet.getMatricule()
                        )
                ))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "success", false,
                                "message", "Aucun vétérinaire trouvé avec matricule : " + matricule
                        )));
    }


    // Helper to get current commercial user id (adapt to your auth)
    private Long getCurrentCommercialId() {
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // return ((CustomUserDetails) auth.getPrincipal()).getId();
        return 999L; // placeholder
    }

    @GetMapping("/cart/{vetMatricule}")
    public ResponseEntity<CommercialCartResponse> getCart(@PathVariable String vetMatricule) {
        return ResponseEntity.ok(
                commercialOrderService.getOrCreateCommercialCart(vetMatricule)
        );
    }


    @PostMapping("/cart/{vetMatricule}/items")
    public ResponseEntity<CartItemDto> addItem(
            @PathVariable String vetMatricule,
            @RequestBody @Valid CommercialCartItemRequest request) {
        return ResponseEntity.ok(commercialOrderService.addItemToCommercialCart(vetMatricule, request, getCurrentCommercialId()));
    }

//    @PatchMapping("/cart/{vetMatricule}/items/{itemId}")
//    public ResponseEntity<CartItemDto> updateItem(
//            @PathVariable String vetMatricule,
//            @PathVariable Long itemId,
//            @RequestParam Integer quantity) {
//
//        return ResponseEntity.ok(commercialOrderService.updateCommercialCartItem(vetMatricule, itemId, req, getCurrentCommercialId()));
//    }

    @PatchMapping("/cart/{vetMatricule}/items/{itemId}")
    public ResponseEntity<CartItemDto> updateItem(
            @PathVariable String vetMatricule,
            @PathVariable Long itemId,
            @RequestBody CommercialCartItemRequest req) {

        CartItemDto dto = commercialOrderService.updateCommercialCartItem(vetMatricule, itemId, req, getCurrentCommercialId());
        if (dto == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(dto);
    }


    @DeleteMapping("/cart/{vetMatricule}/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable String vetMatricule,
            @PathVariable Long itemId) {
        commercialOrderService.removeCommercialCartItem(vetMatricule, itemId, getCurrentCommercialId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart/{vetMatricule}")
    public ResponseEntity<Void> clearCart(@PathVariable String vetMatricule) {
        commercialOrderService.clearCommercialCart(vetMatricule, getCurrentCommercialId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cart/{vetMatricule}/confirm")
    public ResponseEntity<CommercialOrderConfirmationResponse> confirmOrder(@PathVariable String vetMatricule) {
        return ResponseEntity.ok(commercialOrderService.confirmCommercialOrder(vetMatricule, getCurrentCommercialId()));
    }
}
