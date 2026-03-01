package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.Role;

public record UserProfileResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    Role role,
    boolean deliveryBadge,
    String profileImageUrl
) {}
