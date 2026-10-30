package com.ekart.ekart.dto;

import java.math.BigDecimal;

public record GoalCartItemDto(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String reason,
        boolean inStock
) {
}
