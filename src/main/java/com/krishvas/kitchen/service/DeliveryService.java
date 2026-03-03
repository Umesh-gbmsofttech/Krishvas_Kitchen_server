package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.DeliveryPartnerApplyRequest;
import com.krishvas.kitchen.dto.DeliveryPartnerDecisionRequest;
import com.krishvas.kitchen.entity.*;
import com.krishvas.kitchen.repository.DeliveryPartnerRepository;
import com.krishvas.kitchen.repository.DeliveryTrackingRepository;
import com.krishvas.kitchen.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final OrderRepository orderRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final NotificationService notificationService;

    @Transactional
    public DeliveryPartner apply(User user, DeliveryPartnerApplyRequest request) {
        if (user.getRole() != Role.USER && user.getRole() != Role.DELIVERY_PARTNER) {
            throw new IllegalArgumentException("Only app users can apply");
        }

        DeliveryPartner partner = deliveryPartnerRepository.findByUser(user).orElseGet(() -> {
            DeliveryPartner p = new DeliveryPartner();
            p.setUser(user);
            return p;
        });

        partner.setVehicleType(request.vehicleType());
        partner.setVehicleNumber(request.vehicleNumber());
        partner.setStatus(DeliveryPartnerStatus.PENDING);
        DeliveryPartner saved = deliveryPartnerRepository.save(partner);

        Map<String, Object> adminPayload = new HashMap<>();
        adminPayload.put("event", "NEW_DELIVERY_PARTNER_REQUEST");
        adminPayload.put("userId", user.getId());
        adminPayload.put("name", user.getFullName());
        notificationService.publishRoleEvent("admin", adminPayload);

        return saved;
    }

    public DeliveryPartner myStatus(User user) {
        return deliveryPartnerRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException("No delivery partner application found"));
    }

    public List<DeliveryPartner> pendingRequests() {
        return deliveryPartnerRepository.findByStatusOrderByAppliedAtDesc(DeliveryPartnerStatus.PENDING);
    }

    @Transactional
    public DeliveryPartner decide(Long partnerId, DeliveryPartnerDecisionRequest request) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found"));

        boolean approve = request.approve();
        partner.setVehicleType(request.vehicleType());
        partner.setVehicleNumber(request.vehicleNumber());
        partner.setStatus(approve ? DeliveryPartnerStatus.APPROVED : DeliveryPartnerStatus.REJECTED);
        if (approve) {
            if (request.salaryType() == null || request.salaryAmount() == null || request.salaryAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Salary type and salary amount are required for approval");
            }
            partner.setSalaryType(request.salaryType());
            partner.setSalaryAmount(request.salaryAmount());
            partner.setApprovedAt(Instant.now());
            User user = partner.getUser();
            user.setRole(Role.DELIVERY_PARTNER);
            user.setDeliveryBadge(true);
            user.setDeliveryModeActive(true);
            notificationService.createForUser(
                user,
                NotificationType.DELIVERY_PARTNER_APPROVED,
                "Delivery profile approved",
                "You are now an approved delivery partner.",
                "{\"salaryType\":\"" + request.salaryType() + "\",\"salaryAmount\":\"" + request.salaryAmount() + "\"}"
            );
        } else {
            partner.setRejectedAt(Instant.now());
        }
        return deliveryPartnerRepository.save(partner);
    }

    @Transactional
    public DeliveryPartner updatePartner(Long partnerId, DeliveryPartnerDecisionRequest request) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found"));
        if (request.vehicleType() != null) partner.setVehicleType(request.vehicleType());
        if (request.vehicleNumber() != null) partner.setVehicleNumber(request.vehicleNumber());
        if (request.salaryType() != null) partner.setSalaryType(request.salaryType());
        if (request.salaryAmount() != null && request.salaryAmount().compareTo(BigDecimal.ZERO) > 0) {
            partner.setSalaryAmount(request.salaryAmount());
        }
        return deliveryPartnerRepository.save(partner);
    }

    public List<DeliveryPartner> approvedPartners() {
        return deliveryPartnerRepository.findByStatusOrderByAppliedAtDesc(DeliveryPartnerStatus.APPROVED).stream()
            .filter(partner -> partner.getUser() != null && partner.getUser().isDeliveryModeActive())
            .toList();
    }

    public Map<String, Object> dashboard(User deliveryUser) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUser(deliveryUser)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner profile not found"));

        List<Order> deliveries = orderRepository.findByDeliveryPartnerOrderByCreatedAtDesc(partner);
        List<Order> recentDeliveries = deliveries.stream().limit(50).toList();

        Instant now = Instant.now();
        long todayCount = deliveries.stream().filter(o -> o.getCreatedAt().isAfter(now.minus(1, ChronoUnit.DAYS))).count();
        long last7Count = deliveries.stream().filter(o -> o.getCreatedAt().isAfter(now.minus(7, ChronoUnit.DAYS))).count();
        long last30Count = deliveries.stream().filter(o -> o.getCreatedAt().isAfter(now.minus(30, ChronoUnit.DAYS))).count();

        BigDecimal distance30Days = deliveryTrackingRepository
            .findByDeliveryPartnerAndTimestampAfter(partner, now.minus(30, ChronoUnit.DAYS))
            .stream()
            .map(DeliveryTracking::getDistanceKm)
            .filter(d -> d != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal earnings = BigDecimal.ZERO;
        if (partner.getSalaryType() == DeliverySalaryType.FIXED_DAILY && partner.getSalaryAmount() != null) {
            long activeDays = deliveries.stream()
                .filter(o -> o.getCreatedAt().isAfter(now.minus(30, ChronoUnit.DAYS)))
                .map(o -> o.getCreatedAt().truncatedTo(ChronoUnit.DAYS))
                .distinct()
                .count();
            earnings = partner.getSalaryAmount().multiply(BigDecimal.valueOf(activeDays));
        } else if (partner.getSalaryType() == DeliverySalaryType.FIXED_MONTHLY && partner.getSalaryAmount() != null) {
            earnings = partner.getSalaryAmount();
        } else if (partner.getSalaryType() == DeliverySalaryType.PER_KM && partner.getSalaryAmount() != null) {
            earnings = partner.getSalaryAmount().multiply(distance30Days);
        }

        List<Map<String, Object>> compactOrders = recentDeliveries.stream()
            .map(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("orderId", o.getOrderId());
                m.put("status", o.getStatus());
                m.put("addressLine", o.getAddressLine());
                m.put("apartmentOrSociety", o.getApartmentOrSociety());
                m.put("flatNumber", o.getFlatNumber());
                m.put("latitude", o.getLatitude());
                m.put("longitude", o.getLongitude());
                m.put("createdAt", o.getCreatedAt());
                m.put("deliveryOtp", o.getDeliveryOtp());
                List<Map<String, Object>> itemMaps = o.getItems().stream().map(item -> {
                    Map<String, Object> im = new HashMap<>();
                    im.put("id", item.getId());
                    im.put("itemName", item.getItemName());
                    im.put("quantity", item.getQuantity());
                    im.put("unitPrice", item.getUnitPrice());
                    im.put("totalPrice", item.getTotalPrice());
                    if (item.getMenuItem() != null) {
                        im.put("menuItemId", item.getMenuItem().getId());
                        im.put("imageUrl", item.getMenuItem().getImageUrl());
                    }
                    return im;
                }).toList();
                m.put("items", itemMaps);
                return m;
            })
            .sorted(Comparator.comparing((Map<String, Object> m) -> (Instant) m.get("createdAt"), Comparator.nullsLast(Comparator.naturalOrder())).reversed())
            .collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("todayDeliveries", todayCount);
        map.put("last7DaysDeliveries", last7Count);
        map.put("last30DaysDeliveries", last30Count);
        map.put("olderHistoryMonthBack", last30Count);
        map.put("distanceKm", distance30Days);
        map.put("deliveries", compactOrders);
        map.put("salaryType", partner.getSalaryType());
        map.put("salaryAmount", partner.getSalaryAmount());
        map.put("estimatedEarnings", earnings);
        return map;
    }
}
