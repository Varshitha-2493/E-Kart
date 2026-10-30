package com.ekart.ekart.dto;

import java.util.List;

public record ReviewSummaryResponse(
        Long productId,
        String productName,
        int reviewCount,
        List<String> likes,
        List<String> complaints,
        String overall
) {
}
