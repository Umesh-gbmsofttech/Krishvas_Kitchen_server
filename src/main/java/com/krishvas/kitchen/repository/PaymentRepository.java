package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.Order;
import com.krishvas.kitchen.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder(Order order);
}
