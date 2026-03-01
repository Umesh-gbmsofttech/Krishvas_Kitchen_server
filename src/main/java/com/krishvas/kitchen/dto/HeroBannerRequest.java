package com.krishvas.kitchen.dto;

public record HeroBannerRequest(
    String title,
    String imageUrl,
    String actionLabel,
    Integer positionOrder,
    boolean active
) {}
