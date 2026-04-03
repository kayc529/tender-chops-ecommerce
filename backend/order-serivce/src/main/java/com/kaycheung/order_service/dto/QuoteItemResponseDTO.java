package com.kaycheung.order_service.dto;

import java.util.UUID;

public record QuoteItemResponseDTO(
        UUID quoteItemId,
        UUID productId,
        String productTitle,
        Long unitPrice,
        String displayUnitPrice,
        int quantity,
        Long lineTotalAmount,
        String displayLineTotalAmount
) {
}
