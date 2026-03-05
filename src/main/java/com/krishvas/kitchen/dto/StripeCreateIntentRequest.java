package com.krishvas.kitchen.dto;

public record StripeCreateIntentRequest(
    Long amountMinor,
    String currency,
    String orderRef,
    String paymentMethodPreference
) {}
