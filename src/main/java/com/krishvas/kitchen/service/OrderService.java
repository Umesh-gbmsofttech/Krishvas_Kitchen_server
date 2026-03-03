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
        LocalDate selectedOrderDate = request.orderDate() == null ? LocalDate.now() : request.orderDate();
        if (selectedOrderDate.isBefore(LocalDate.now()) || selectedOrderDate.isAfter(LocalDate.now().plusDays(6))) {
            throw new IllegalArgumentException("Orders can only be placed for today or up to 7 days ahead");
        }
        order.setOrderDate(selectedOrderDate);
        String orderSlot = request.orderSlot() == null ? "ALL" : request.orderSlot().trim().toUpperCase();
        if (!List.of("ALL", "BREAKFAST", "LUNCH", "DINNER").contains(orderSlot)) {
            orderSlot = "ALL";
        }
        order.setOrderSlot(orderSlot);
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

    public List<Map<String, Object>> allOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toOrderSummary).collect(Collectors.toList());
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
        payload.put("updatedAt", saved.getUpdatedAt());
        notificationService.publishRoleEvent("admin", payload);
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
        if (partner.getUser() == null || !partner.getUser().isDeliveryModeActive()) {
            throw new IllegalArgumentException("Selected delivery partner is currently in customer mode");
        }
        if (order.getOrderDate() != null && !order.getOrderDate().isEqual(LocalDate.now())) {
            throw new IllegalArgumentException("Delivery partner can only be assigned for today's orders");
        }
        order.setDeliveryPartner(partner);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setDeliveryOtp(generateDeliveryOtp());
        order.setDeliveryAcceptedAt(null);
        order.setUpdatedAt(java.time.Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.createForUser(
            partner.getUser(),
            NotificationType.DELIVERY_ASSIGNED,
            "Delivery request assigned",
            "You have a new delivery request: " + orderId,
            "{\"orderId\":\"" + orderId + "\"}"
        );
        notificationService.createForUser(
            saved.getUser(),
            NotificationType.ORDER_STATUS,
            "Delivery partner assigned",
            "Order " + orderId + " was assigned to a delivery partner and is waiting for pickup confirmation.",
            "{\"orderId\":\"" + orderId + "\",\"status\":\"CONFIRMED\"}"
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "DELIVERY_ASSIGNED");
        payload.put("orderId", orderId);
        payload.put("status", OrderStatus.CONFIRMED);
        payload.put("deliveryPartnerId", partner.getId());
        notificationService.publishRoleEvent("admin", payload);
        return saved;
    }

    @Transactional
    public Order acceptAssignedDelivery(String orderId, User deliveryUser) {
        Order order = getByOrderId(orderId);
        if (order.getDeliveryPartner() == null || order.getDeliveryPartner().getUser() == null) {
            throw new IllegalArgumentException("No delivery partner assigned");
        }
        if (!order.getDeliveryPartner().getUser().getId().equals(deliveryUser.getId())) {
            throw new IllegalArgumentException("Only assigned delivery partner can accept this request");
        }
        order.setDeliveryAcceptedAt(Instant.now());
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        notificationService.createForUser(
            saved.getUser(),
            NotificationType.ORDER_STATUS,
            "Your order is on the way",
            "Order " + orderId + " is out for delivery. OTP: " + saved.getDeliveryOtp(),
            "{\"orderId\":\"" + orderId + "\",\"otp\":\"" + saved.getDeliveryOtp() + "\",\"status\":\"OUT_FOR_DELIVERY\"}"
        );
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ORDER_STATUS");
        payload.put("orderId", orderId);
        payload.put("status", OrderStatus.OUT_FOR_DELIVERY);
        payload.put("deliveryAccepted", true);
        payload.put("updatedAt", saved.getUpdatedAt());
        notificationService.publishRoleEvent("admin", payload);
        notificationService.publishOrderEvent(orderId, payload);
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
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ORDER_STATUS");
        payload.put("orderId", orderId);
        payload.put("status", OrderStatus.DELIVERED);
        payload.put("updatedAt", saved.getUpdatedAt());
        notificationService.publishRoleEvent("admin", payload);
        notificationService.publishOrderEvent(orderId, payload);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> assignedOrders(User deliveryUser) {
        if (!deliveryUser.isDeliveryModeActive()) {
            return List.of();
        }
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
            map.put("deliveryAccepted", order.getDeliveryAcceptedAt() != null);
            map.put("orderDate", order.getOrderDate());
            map.put("orderSlot", order.getOrderSlot());
            map.put("createdAt", order.getCreatedAt());
            map.put("customerName", order.getUser() != null ? order.getUser().getFullName() : null);
            map.put("customerPhone", order.getUser() != null ? order.getUser().getPhone() : null);
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toOrderSummary(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderId", order.getOrderId());
        map.put("status", order.getStatus());
        map.put("totalAmount", order.getTotalAmount());
        map.put("paymentMethod", order.getPaymentMethod());
        map.put("paymentStatus", order.getPaymentStatus());
        map.put("addressLine", order.getAddressLine());
        map.put("orderDate", order.getOrderDate());
        map.put("orderSlot", order.getOrderSlot());
        map.put("apartmentOrSociety", order.getApartmentOrSociety());
        map.put("flatNumber", order.getFlatNumber());
        map.put("latitude", order.getLatitude());
        map.put("longitude", order.getLongitude());
        map.put("createdAt", order.getCreatedAt());
        map.put("deliveryAccepted", order.getDeliveryAcceptedAt() != null);
        if (order.getDeliveryPartner() != null) {
            Map<String, Object> partner = new HashMap<>();
            partner.put("id", order.getDeliveryPartner().getId());
            partner.put("vehicleType", order.getDeliveryPartner().getVehicleType());
            partner.put("vehicleNumber", order.getDeliveryPartner().getVehicleNumber());
            if (order.getDeliveryPartner().getUser() != null) {
                Map<String, Object> partnerUser = new HashMap<>();
                partnerUser.put("id", order.getDeliveryPartner().getUser().getId());
                partnerUser.put("fullName", order.getDeliveryPartner().getUser().getFullName());
                partnerUser.put("email", order.getDeliveryPartner().getUser().getEmail());
                partnerUser.put("phone", order.getDeliveryPartner().getUser().getPhone());
                partnerUser.put("profileImageId", order.getDeliveryPartner().getUser().getProfileImageId());
                partner.put("user", partnerUser);
            }
            map.put("deliveryPartner", partner);
        }
        return map;
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
