package com.ekart.ekart.dto;

public record RegisterRequest(
        String name,
        String email,
        String password
) {
}