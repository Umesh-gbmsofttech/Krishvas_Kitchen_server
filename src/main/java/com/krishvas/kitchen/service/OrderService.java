package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.OrderItemRequest;
import com.krishvas.kitchen.dto.PlaceOrderRequest;
import com.krishvas.kitchen.dto.VerifyDeliveryOtpRequest;
import com.krishvas.kitchen.entity.*;
import com.krishvas.kitchen.repository.DeliveryPartnerRepository;
import com.krishvas.kitchen.repository.MenuItemRepository;
import com.krishvas.kitchen.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final NotificationService notificationService;

    @Transactional
    public Order placeOrder(User user, PlaceOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Order order = new Order();
        order.setOrderId(generateOrderId());
        order.setUser(user);
        order.setApartmentOrSociety(request.apartmentOrSociety());
        order.setFlatNumber(request.flatNumber());
        order.setAddressLine(request.addressLine());
        order.setLatitude(request.latitude());
        order.setLongitude(request.longitude());
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(request.paymentMethod() == PaymentMethod.COD ? PaymentStatus.PENDING : PaymentStatus.SUCCESS);
        order.setNotes(request.notes());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            if (itemRequest.menuItemId() != null) {
                menuItemRepository.findById(itemRequest.menuItemId()).ifPresent(item::setMenuItem);
            }
            item.setItemName(itemRequest.itemName());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            BigDecimal lineTotal = itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            item.setTotalPrice(lineTotal);
            total = total.add(lineTotal);
            order.getItems().add(item);
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("event", "NEW_ORDER");
        adminPayload.put("orderId", saved.getOrderId());
        adminPayload.put("amount", saved.getTotalAmount());
        adminPayload.put("status", saved.getStatus());
        notificationService.publishRoleEvent("admin", adminPayload);

        notificationService.createForUser(
            user,
            NotificationType.ORDER_CREATED,
            "Order placed",
            "Your order " + saved.getOrderId() + " has been placed successfully.",
            "{\"orderId\":\"" + saved.getOrderId() + "\"}"
        );

        return saved;
    }

    public List<Order> userOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Order> allOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Order getByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional
    public Order updateStatus(String orderId, OrderStatus status) {
        Order order = getByOrderId(orderId);
        order.setStatus(status);
        order.setUpdatedAt(java.time.Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.createForUser(
            saved.getUser(),
            NotificationType.ORDER_STATUS,
            "Order status updated",
            "Order " + orderId + " is now " + status,
            "{\"orderId\":\"" + orderId + "\",\"status\":\"" + status + "\"}"
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ORDER_STATUS");
        payload.put("orderId", orderId);
        payload.put("status", status);
        notificationService.publishOrderEvent(orderId, payload);
        return saved;
    }

    @Transactional
    public Order assignDelivery(String orderId, Long deliveryPartnerId) {
        Order order = getByOrderId(orderId);
        DeliveryPartner partner = deliveryPartnerRepository.findById(deliveryPartnerId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found"));
        if (partner.getStatus() != DeliveryPartnerStatus.APPROVED) {
            throw new IllegalArgumentException("Delivery partner is not approved");
        }
        order.setDeliveryPartner(partner);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDeliveryOtp(generateDeliveryOtp());
        order.setUpdatedAt(java.time.Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.createForUser(
            partner.getUser(),
            NotificationType.DELIVERY_ASSIGNED,
            "Delivery assigned",
            "You have a new delivery: " + orderId,
            "{\"orderId\":\"" + orderId + "\"}"
        );
        notificationService.createForUser(
            saved.getUser(),
            NotificationType.ORDER_STATUS,
            "Your order is on the way",
            "Order " + orderId + " is out for delivery. OTP: " + saved.getDeliveryOtp(),
            "{\"orderId\":\"" + orderId + "\",\"otp\":\"" + saved.getDeliveryOtp() + "\"}"
        );
        return saved;
    }

    @Transactional
    public Order verifyDeliveryOtp(String orderId, User deliveryUser, VerifyDeliveryOtpRequest request) {
        Order order = getByOrderId(orderId);
        if (order.getDeliveryPartner() == null || order.getDeliveryPartner().getUser() == null) {
            throw new IllegalArgumentException("No delivery partner assigned");
        }
        if (!order.getDeliveryPartner().getUser().getId().equals(deliveryUser.getId())) {
            throw new IllegalArgumentException("Only assigned delivery partner can verify OTP");
        }
        if (request == null || request.otp() == null || request.otp().isBlank()) {
            throw new IllegalArgumentException("OTP is required");
        }
        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(request.otp().trim())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        order.setStatus(OrderStatus.DELIVERED);
        order.setOtpVerifiedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.createForUser(
            saved.getUser(),
            NotificationType.ORDER_STATUS,
            "Order delivered",
            "Order " + orderId + " has been delivered successfully.",
            "{\"orderId\":\"" + orderId + "\",\"status\":\"DELIVERED\"}"
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignedOrders(User deliveryUser) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUser(deliveryUser)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner profile not found"));
        return orderRepository.findByDeliveryPartnerOrderByCreatedAtDesc(partner).stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderId", order.getOrderId());
            map.put("status", order.getStatus());
            map.put("totalAmount", order.getTotalAmount());
            map.put("addressLine", order.getAddressLine());
            map.put("apartmentOrSociety", order.getApartmentOrSociety());
            map.put("flatNumber", order.getFlatNumber());
            map.put("latitude", order.getLatitude());
            map.put("longitude", order.getLongitude());
            map.put("deliveryOtp", order.getDeliveryOtp());
            map.put("createdAt", order.getCreatedAt());
            map.put("customerName", order.getUser() != null ? order.getUser().getFullName() : null);
            map.put("customerPhone", order.getUser() != null ? order.getUser().getPhone() : null);
            return map;
        }).collect(Collectors.toList());
    }

    private String generateDeliveryOtp() {
        int otp = 1000 + (int) (Math.random() * 9000);
        return String.valueOf(otp);
    }

    private String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "KK-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
