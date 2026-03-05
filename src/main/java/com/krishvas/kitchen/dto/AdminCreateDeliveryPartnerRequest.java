package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.DeliverySalaryType;

import java.math.BigDecimal;

public record AdminCreateDeliveryPartnerRequest(
    String fullName,
    String email,
    String password,
    String phone,
    String vehicleType,
    String vehicleNumber,
    DeliverySalaryType salaryType,
    BigDecimal salaryAmount
) {}
