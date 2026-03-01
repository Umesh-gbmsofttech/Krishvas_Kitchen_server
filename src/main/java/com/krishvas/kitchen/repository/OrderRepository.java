package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.Order;
import com.krishvas.kitchen.entity.OrderStatus;
import com.krishvas.kitchen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
