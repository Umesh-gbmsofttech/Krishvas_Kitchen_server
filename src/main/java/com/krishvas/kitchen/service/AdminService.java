package com.krishvas.kitchen.service;

import com.krishvas.kitchen.entity.Admin;
import com.krishvas.kitchen.entity.OrderStatus;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.AdminRepository;
import com.krishvas.kitchen.repository.OrderRepository;
import com.krishvas.kitchen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryService deliveryService;

    @Transactional
    public Admin ensureAdmin(User user) {
        return adminRepository.findByUser(user).orElseGet(() -> {
            Admin admin = new Admin();
            admin.setUser(user);
            return adminRepository.save(admin);
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(User adminUser) {
        Admin admin = ensureAdmin(adminUser);
        List<com.krishvas.kitchen.entity.Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant tomorrowStart = todayStart.plus(1, ChronoUnit.DAYS);

        long todayOrders = allOrders.stream()
            .filter(order -> order.getCreatedAt() != null)
            .filter(order -> !order.getCreatedAt().isBefore(todayStart) && order.getCreatedAt().isBefore(tomorrowStart))
            .count();
        BigDecimal todayEarnings = allOrders.stream()
            .filter(order -> order.getCreatedAt() != null)
            .filter(order -> !order.getCreatedAt().isBefore(todayStart) && order.getCreatedAt().isBefore(tomorrowStart))
            .map(order -> order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalOrders", allOrders.size());
        payload.put("todayOrders", todayOrders);
        payload.put("todayEarnings", todayEarnings);
        payload.put("totalUsers", userRepository.count());
        payload.put("pendingDeliveryPartners", deliveryService.pendingRequests().size());
        payload.put("activeDrivers", deliveryService.approvedPartners().size());
        payload.put("placedOrders", orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PLACED).size());
        payload.put("preparingOrders", orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PREPARING).size());
        payload.put("readyOrders", orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.CONFIRMED).size());
        payload.put("onWayOrders", orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.OUT_FOR_DELIVERY).size());
        payload.put("kitchenActive", admin.isKitchenActive());
        payload.put("darkMode", admin.isDarkMode());
        return payload;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> settings(User adminUser) {
        Admin admin = ensureAdmin(adminUser);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kitchenActive", admin.isKitchenActive());
        payload.put("darkMode", admin.isDarkMode());
        return payload;
    }

    @Transactional
    public Map<String, Object> updateSettings(User adminUser, Boolean kitchenActive, Boolean darkMode) {
        Admin admin = ensureAdmin(adminUser);
        if (kitchenActive != null) {
            admin.setKitchenActive(kitchenActive);
        }
        if (darkMode != null) {
            admin.setDarkMode(darkMode);
        }
        adminRepository.save(admin);
        return settings(adminUser);
    }
}
