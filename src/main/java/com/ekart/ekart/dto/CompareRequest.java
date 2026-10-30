package com.ekart.ekart.dto;

import java.util.List;

public record CompareRequest(
        List<Long> productIds,
        String preference
) {
}
