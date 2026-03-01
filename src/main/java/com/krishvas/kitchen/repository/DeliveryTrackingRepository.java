package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.DeliveryPartner;
import com.krishvas.kitchen.entity.DeliveryTracking;
import com.krishvas.kitchen.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {
    List<DeliveryTracking> findByOrderOrderByTimestampAsc(Order order);
    List<DeliveryTracking> findByDeliveryPartnerAndTimestampAfter(DeliveryPartner deliveryPartner, Instant timestamp);
}
