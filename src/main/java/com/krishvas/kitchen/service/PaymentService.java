package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.PaymentRequest;
import com.krishvas.kitchen.entity.Order;
import com.krishvas.kitchen.entity.Payment;
import com.krishvas.kitchen.entity.PaymentStatus;
import com.krishvas.kitchen.repository.OrderRepository;
import com.krishvas.kitchen.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment record(String orderId, PaymentRequest request) {
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(request.method());
        payment.setProvider(request.provider());
        payment.setTransactionRef(request.transactionRef());
        payment.setStatus(PaymentStatus.SUCCESS);

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order);

        return paymentRepository.save(payment);
    }
}
