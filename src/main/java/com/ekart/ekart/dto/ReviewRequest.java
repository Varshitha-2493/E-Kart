package com.ekart.ekart.dto;

public record ReviewRequest(
        Long userId,
        Integer rating,
        String comment
) {
}
