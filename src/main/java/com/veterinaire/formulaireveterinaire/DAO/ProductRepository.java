package com.veterinaire.formulaireveterinaire.DAO;

import com.veterinaire.formulaireveterinaire.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
    List<Product> findBySubCategory(String subCategory);
    List<Product> findByInStock(Boolean inStock);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants")
    List<Product> findAllWithVariants();
}
