package com.krishvas.kitchen.dto;

import java.time.LocalDate;
import java.util.List;

public record MenuRequest(
    String title,
    String description,
    LocalDate scheduleDate,
    boolean template,
    List<MenuItemPayload> items
) {}
