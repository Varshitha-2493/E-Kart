package com.ekart.ekart.dto;

import com.ekart.ekart.model.OrderStatus;

public record OrderStatusUpdateRequest(
        OrderStatus status
) {
}
