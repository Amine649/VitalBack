package com.veterinaire.formulaireveterinaire.DAO.Cart;

import com.veterinaire.formulaireveterinaire.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    Optional<OrderItem> findByOrderIdAndProductIdAndVariantId(Long orderId, Long productId, Long variantId);
}
