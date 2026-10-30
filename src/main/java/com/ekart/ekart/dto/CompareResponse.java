package com.ekart.ekart.dto;

import java.util.List;

public record CompareResponse(
        Long recommendedProductId,
        String recommendedProductName,
        String preference,
        String reasoning,
        List<String> comparisonPoints
) {
}
