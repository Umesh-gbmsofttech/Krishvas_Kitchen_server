package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.DeliveryPartnerApplyRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return deliveryPartnerRepository.findByStatus(DeliveryPartnerStatus.PENDING);
    }

    @Transactional
    public DeliveryPartner decide(Long partnerId, boolean approve) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found"));

        partner.setStatus(approve ? DeliveryPartnerStatus.APPROVED : DeliveryPartnerStatus.REJECTED);
        if (approve) {
            partner.setApprovedAt(Instant.now());
            User user = partner.getUser();
            user.setRole(Role.DELIVERY_PARTNER);
            user.setDeliveryBadge(true);
            notificationService.createForUser(
                user,
                NotificationType.DELIVERY_PARTNER_APPROVED,
                "Delivery profile approved",
                "You are now an approved delivery partner.",
                "{}"
            );
        } else {
            partner.setRejectedAt(Instant.now());
        }
        return deliveryPartnerRepository.save(partner);
    }

    public Map<String, Object> dashboard(User deliveryUser) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUser(deliveryUser)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner profile not found"));

        List<Order> deliveries = orderRepository.findAll().stream()
            .filter(o -> o.getDeliveryPartner() != null && o.getDeliveryPartner().getId().equals(partner.getId()))
            .toList();

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

        Map<String, Object> map = new HashMap<>();
        map.put("todayDeliveries", todayCount);
        map.put("last7DaysDeliveries", last7Count);
        map.put("last30DaysDeliveries", last30Count);
        map.put("olderHistoryMonthBack", last30Count);
        map.put("distanceKm", distance30Days);
        map.put("deliveries", deliveries);
        return map;
    }
}
