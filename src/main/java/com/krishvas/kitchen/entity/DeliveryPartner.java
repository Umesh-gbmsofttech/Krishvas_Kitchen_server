package com.krishvas.kitchen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    @Enumerated(EnumType.STRING)
    private DeliverySalaryType salaryType;

    @Column(precision = 10, scale = 2)
    private BigDecimal salaryAmount;

    private Instant approvedAt;
    private Instant rejectedAt;

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();
}
