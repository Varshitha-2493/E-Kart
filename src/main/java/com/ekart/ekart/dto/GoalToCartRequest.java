package com.ekart.ekart.dto;

import java.math.BigDecimal;

public record GoalToCartRequest(
        String goal,
        BigDecimal budget,
        Long userId,
        boolean addToCart
) {
}
