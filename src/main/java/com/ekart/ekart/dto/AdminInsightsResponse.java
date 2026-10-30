package com.ekart.ekart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminInsightsResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        long totalProducts,
        long lowStockCount,
        long pendingOrders,
        Map<String, Long> ordersByStatus,
        List<CategorySalesDto> salesByCategory,
        List<NamedCountDto> topProducts,
        List<NamedCountDto> lowStockItems,
        String aiSummary
) {
}
