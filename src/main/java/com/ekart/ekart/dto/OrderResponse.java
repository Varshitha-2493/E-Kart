package com.ekart.ekart.dto;

import com.ekart.ekart.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        OrderStatus status,
        String shippingAddress,
        LocalDate deliveryDate,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}