package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PlaceOrderRequest(
    String apartmentOrSociety,
    String flatNumber,
    String addressLine,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDate orderDate,
    String orderSlot,
    PaymentMethod paymentMethod,
    String notes,
    List<OrderItemRequest> items
) {}
