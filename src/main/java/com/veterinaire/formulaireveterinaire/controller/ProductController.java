package com.veterinaire.formulaireveterinaire.controller;

import com.veterinaire.formulaireveterinaire.entity.Product;
import com.veterinaire.formulaireveterinaire.entity.ProductVariant;
import com.veterinaire.formulaireveterinaire.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/all")
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/add")
    public Product createProduct(@RequestBody Product product) {
        return productService.createProduct(product);
    }


    @PutMapping("/update/{id}/full")
    public ResponseEntity<Product> updateProductWithVariants(
            @PathVariable Long id,
            @RequestBody Product product) {
        try {
            Product updated = productService.updateProductWithVariants(id, product);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(@PathVariable String category) {
        return productService.getProductsByCategory(category);
    }

    @GetMapping("/subcategory/{subCategory}")
    public List<Product> getProductsBySubCategory(@PathVariable String subCategory) {
        return productService.getProductsBySubCategory(subCategory);
    }

    @GetMapping("/stock/{inStock}")
    public List<Product> getProductsByStockStatus(@PathVariable Boolean inStock) {
        return productService.getProductsByStockStatus(inStock);
    }


    @GetMapping("/{productId}/variants")
    public List<ProductVariant> getVariants(@PathVariable Long productId) {
        return productService.getVariantsByProduct(productId);
    }

    // ✅ NEW — Add a variant to a product
    // Body: { "packaging": "1kg", "price": 15.00, "inStock": true }
    @PostMapping("/{productId}/variants/add")
    public ResponseEntity<ProductVariant> addVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariant variant) {
        try {
            ProductVariant saved = productService.addVariant(productId, variant);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ NEW — Delete a specific variant
    @DeleteMapping("/variants/delete/{variantId}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long variantId) {
        productService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
