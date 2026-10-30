package com.ekart.ekart.dto;

import java.math.BigDecimal;
import java.util.List;

public record GoalToCartResponse(
        String goal,
        BigDecimal budget,
        BigDecimal total,
        boolean withinBudget,
        boolean addedToCart,
        String aiNotes,
        List<GoalCartItemDto> items,
        List<String> validationNotes
) {
}
