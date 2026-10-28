package com.ekart.ekart.dto;

import com.ekart.ekart.model.Role;

public record AuthResponse(
        Long id,
        String name,
        String email,
        Role role,
        String message
) {
}