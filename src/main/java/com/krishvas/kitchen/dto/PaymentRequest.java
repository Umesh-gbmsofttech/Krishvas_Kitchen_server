package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.PaymentMethod;

public record PaymentRequest(
    PaymentMethod method,
    String provider,
    String transactionRef
) {}
