package com.veterinaire.formulaireveterinaire.entity;

import com.veterinaire.formulaireveterinaire.Enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class CartOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CART;

    @Column(name = "order_number", unique = true)
    private String orderNumber;               // NULL for carts

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;


    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;        // NULL for carts

    @Column(name = "vet_matricule")
    private String vetMatricule;

    @Column(name = "ordered_by_commercial")
    private Boolean orderedByCommercial = false;

}