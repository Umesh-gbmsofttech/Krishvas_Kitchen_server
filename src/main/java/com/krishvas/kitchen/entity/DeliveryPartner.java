package com.krishvas.kitchen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "delivery_partners")
public class DeliveryPartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryPartnerStatus status = DeliveryPartnerStatus.PENDING;

    private String vehicleType;
    private String vehicleNumber;

    private Instant approvedAt;
    private Instant rejectedAt;

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();
}
