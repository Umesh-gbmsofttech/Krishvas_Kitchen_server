package com.krishvas.kitchen.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String fullName,
    @Email String email,
    @NotBlank String password,
    String phone
) {}
