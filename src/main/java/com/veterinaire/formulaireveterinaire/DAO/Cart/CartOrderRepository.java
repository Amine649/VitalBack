package com.veterinaire.formulaireveterinaire.DAO.Cart;

import com.veterinaire.formulaireveterinaire.Enums.OrderStatus;
import com.veterinaire.formulaireveterinaire.entity.CartOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartOrderRepository extends JpaRepository<CartOrder, Long> {
    Optional<CartOrder> findByUserIdAndStatus(Long userId, OrderStatus status);

    // Add this one (very useful for commercial flow)
    Optional<CartOrder> findByVetMatriculeAndStatusAndOrderedByCommercial(
            String vetMatricule,
            OrderStatus status,
            Boolean orderedByCommercial
    );
}
