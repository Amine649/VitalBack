package com.veterinaire.formulaireveterinaire.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore  // avoid infinite loop
    private Product product;

    @Column(nullable = false)
    private String packaging; // "1kg", "3kg", "500g"...

    @Column(nullable = false)
    private BigDecimal price;
}
