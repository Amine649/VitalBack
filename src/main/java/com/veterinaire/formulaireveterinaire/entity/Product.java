package com.veterinaire.formulaireveterinaire.entity;


import com.veterinaire.formulaireveterinaire.Enums.Category;
import com.veterinaire.formulaireveterinaire.Enums.SubCategory;
import com.veterinaire.formulaireveterinaire.Enums.SubSubCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

//    @Column(nullable = false)
//    private BigDecimal price;

    @Column(nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubCategory subCategory;

    @Enumerated(EnumType.STRING)
    @Column
    private SubSubCategory subSubCategory;  // can be null for TEST_RAPIDE and COMPLEMENT

    @Column(nullable = false)
    private Boolean inStock;

    @Column
    private String detailsUrl;

    // ✅ NEW
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    // ✅ NEW helper
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }
}
