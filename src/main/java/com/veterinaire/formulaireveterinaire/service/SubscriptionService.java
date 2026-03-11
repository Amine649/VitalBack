package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.Enums.SubscriptionType;
import com.veterinaire.formulaireveterinaire.DTO.SubscriptionDTO;

import java.util.List;
import java.util.Optional;

public interface SubscriptionService {
    String assignSubscription(Long userId, SubscriptionType subscriptionType);
    String updateSubscription(Long subscriptionId, SubscriptionType subscriptionType);
    String deleteSubscription(Long subscriptionId);
    List<SubscriptionDTO> getAllSubscriptions();
    Optional<SubscriptionDTO> getSubscriptionById(Long subscriptionId);
}
