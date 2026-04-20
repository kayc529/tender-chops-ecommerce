package com.kaycheung.order_service.dto;

import java.util.UUID;

public record OrderItemResponseDTO(
        UUID orderItemId,
        UUID productId,
        String productTitle,
        Long unitPrice,
        String displayUnitPrice,
        int quantity,
        Long lineTotalAmount,
        String displayLineTotalAmount
) {
}
