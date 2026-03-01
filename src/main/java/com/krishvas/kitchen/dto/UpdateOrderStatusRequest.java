package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.OrderStatus;

public record UpdateOrderStatusRequest(OrderStatus status) {}
