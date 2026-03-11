package com.veterinaire.formulaireveterinaire.service;

import com.veterinaire.formulaireveterinaire.Enums.SubSubCategory;
import com.veterinaire.formulaireveterinaire.entity.Product;
import com.veterinaire.formulaireveterinaire.entity.ProductVariant;

import java.util.*;

public interface ProductService {
    List<Product> getAllProducts();
    Optional<Product> getProductById(Long id);
    Product createProduct(Product product);
    void deleteProduct(Long id);
    List<Product> getProductsByCategory(String category);
    List<Product> getProductsBySubCategory(String subCategory);
    List<Product> getProductsByStockStatus(Boolean inStock);

    ProductVariant addVariant(Long productId, ProductVariant variant);
    Product updateProductWithVariants(Long id, Product product);
    void deleteVariant(Long variantId);
    List<ProductVariant> getVariantsByProduct(Long productId);
}
