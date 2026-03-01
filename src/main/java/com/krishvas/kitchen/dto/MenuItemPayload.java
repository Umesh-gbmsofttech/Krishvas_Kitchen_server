package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.MenuCategory;

import java.math.BigDecimal;

public record MenuItemPayload(
    String name,
    String description,
    BigDecimal price,
    MenuCategory category,
    String imageUrl,
    boolean available
) {}
