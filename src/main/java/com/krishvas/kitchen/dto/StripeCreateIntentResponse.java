package com.krishvas.kitchen.dto;

public record StripeCreateIntentResponse(
    String paymentIntentId,
    String clientSecret,
    String currency,
    Long amountMinor
) {}
