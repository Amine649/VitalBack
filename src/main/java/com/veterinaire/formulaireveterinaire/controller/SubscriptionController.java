package com.veterinaire.formulaireveterinaire.controller;


import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import com.veterinaire.formulaireveterinaire.DTO.SubscriptionDTO;
import com.veterinaire.formulaireveterinaire.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/assign/{userId}")
    public ResponseEntity<Map<String, String>> assignSubscription(
            @PathVariable Long userId,
            @RequestParam SubscriptionType subscriptionType) {

        try {
            String result = subscriptionService.assignSubscription(userId, subscriptionType);
            return ResponseEntity.ok(Map.of("message", result)); // Success: 200 OK with JSON
        } catch (RuntimeException ex) {
            String errorMessage = ex.getMessage();

            if (errorMessage.startsWith("Utilisateur non trouvé")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            } else if (errorMessage.contains("a déjà un abonnement")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Or HttpStatus.CONFLICT
                        .body(Map.of("error", errorMessage));
            } else {
                // Unexpected error
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Une erreur inattendue s'est produite."));
            }
        }
    }


    @PutMapping("/update/{subscriptionId}")
    public ResponseEntity<Map<String, String>> updateSubscription(
            @PathVariable Long subscriptionId,
            @RequestParam SubscriptionType subscriptionType) {

        String result = subscriptionService.updateSubscription(subscriptionId, subscriptionType);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @DeleteMapping("/delete/{subscriptionId}")
    public ResponseEntity<Map<String, String>> deleteSubscription(@PathVariable Long subscriptionId) {
        String result = subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/all")
    public ResponseEntity<List<SubscriptionDTO>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionDTO> getSubscriptionById(@PathVariable Long subscriptionId) {
        return subscriptionService.getSubscriptionById(subscriptionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
