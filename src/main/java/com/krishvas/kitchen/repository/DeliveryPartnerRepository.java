package com.krishvas.kitchen.repository;

import com.krishvas.kitchen.entity.DeliveryPartner;
import com.krishvas.kitchen.entity.DeliveryPartnerStatus;
import com.krishvas.kitchen.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByUser(User user);
    List<DeliveryPartner> findByStatus(DeliveryPartnerStatus status);
}
