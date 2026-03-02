package com.krishvas.kitchen.service;

import com.krishvas.kitchen.dto.TrackingUpdateRequest;
import com.krishvas.kitchen.entity.DeliveryPartner;
import com.krishvas.kitchen.entity.DeliveryTracking;
import com.krishvas.kitchen.entity.Order;
import com.krishvas.kitchen.entity.User;
import com.krishvas.kitchen.repository.DeliveryPartnerRepository;
import com.krishvas.kitchen.repository.DeliveryTrackingRepository;
import com.krishvas.kitchen.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryTrackingRepository deliveryTrackingRepository;
    private final NotificationService notificationService;

    @Transactional
    public DeliveryTracking push(String orderId, User user, TrackingUpdateRequest request) {
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        DeliveryPartner partner = deliveryPartnerRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException("Delivery partner profile not found"));
        if (order.getDeliveryPartner() == null || !order.getDeliveryPartner().getId().equals(partner.getId())) {
            throw new IllegalArgumentException("You are not assigned to this order");
        }

        DeliveryTracking tracking = new DeliveryTracking();
        tracking.setOrder(order);
        tracking.setDeliveryPartner(partner);
        tracking.setLatitude(request.latitude());
        tracking.setLongitude(request.longitude());
        tracking.setDistanceKm(request.distanceKm());
        tracking.setStatusNote(request.statusNote());
        DeliveryTracking saved = deliveryTrackingRepository.save(tracking);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "LOCATION_UPDATE");
        payload.put("orderId", orderId);
        payload.put("latitude", request.latitude());
        payload.put("longitude", request.longitude());
        payload.put("timestamp", saved.getTimestamp());
        payload.put("note", request.statusNote());
        notificationService.publishOrderEvent(orderId, payload);

        return saved;
    }

    public List<DeliveryTracking> history(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return deliveryTrackingRepository.findByOrderOrderByTimestampAsc(order);
    }
}
