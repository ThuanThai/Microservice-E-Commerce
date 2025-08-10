package org.ecommerce.order.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    // Additional query methods can be defined here if needed
}
