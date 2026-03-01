package com.krishvas.kitchen.dto;

import com.krishvas.kitchen.entity.Role;

public record AuthResponse(
    Long userId,
    String fullName,
    String email,
    Role role,
    String token
) {}
