package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.MenuCategory;

import java.math.BigDecimal;

public record OrderItemRequest(
    Long menuItemId,
    String itemName,
    Integer quantity,
    BigDecimal unitPrice,
    MenuCategory category
) {}
