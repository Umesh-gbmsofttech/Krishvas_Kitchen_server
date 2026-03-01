package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
