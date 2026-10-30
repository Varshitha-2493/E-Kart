package com.ekart.ekart.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
