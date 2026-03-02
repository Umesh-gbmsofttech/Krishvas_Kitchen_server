package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.DeliverySalaryType;

import java.math.BigDecimal;

public record DeliveryPartnerDecisionRequest(
    boolean approve,
    String vehicleType,
    String vehicleNumber,
    DeliverySalaryType salaryType,
    BigDecimal salaryAmount
) {}
