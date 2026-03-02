package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.Order;
import com.krishvas.kitchen.entity.OrderStatus;
import com.krishvas.kitchen.entity.DeliveryPartner;
import com.krishvas.kitchen.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.menuItem", "deliveryPartner", "deliveryPartner.user"})
    Optional<Order> findByOrderId(String orderId);

    @EntityGraph(attributePaths = {"items", "items.menuItem", "deliveryPartner", "deliveryPartner.user"})
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"items", "items.menuItem", "deliveryPartner", "deliveryPartner.user"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.menuItem", "deliveryPartner", "deliveryPartner.user"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.menuItem", "deliveryPartner", "deliveryPartner.user"})
    List<Order> findByDeliveryPartnerOrderByCreatedAtDesc(DeliveryPartner deliveryPartner);
}
