package com.krishvas.kitchen.dto;

import java.math.BigDecimal;

public record TrackingUpdateRequest(
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal distanceKm,
    String statusNote
) {}
