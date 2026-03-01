package com.krishvas.kitchen.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "hero_banners")
public class HeroBanner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String imageUrl;

    private String actionLabel;

    @Column(nullable = false)
    private Integer positionOrder;

    @Column(nullable = false)
    private boolean active = true;
}
