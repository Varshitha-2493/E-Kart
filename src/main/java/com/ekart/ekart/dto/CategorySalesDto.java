package com.ekart.ekart.dto;

import java.math.BigDecimal;

public record CategorySalesDto(
        String category,
        BigDecimal amount
) {
}
