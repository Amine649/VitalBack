package com.veterinaire.formulaireveterinaire.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionDTO {
    private Long id;
    private UserDTO user;
    private String subscriptionType;
    private LocalDateTime startDate;
    private LocalDateTime  endDate;
}
