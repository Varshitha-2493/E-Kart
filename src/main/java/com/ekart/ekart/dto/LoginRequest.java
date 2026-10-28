package com.ekart.ekart.dto;

public record LoginRequest(
        String email,
        String password
) {
}