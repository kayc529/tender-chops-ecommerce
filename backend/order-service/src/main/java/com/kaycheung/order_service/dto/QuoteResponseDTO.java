package com.kaycheung.order_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteResponseDTO(
        UUID quoteId,
        UUID sourceCartId,
        String currency,
        Long totalAmount,
        String displayTotalAmount,
        Long sourceCartVersion,
        Instant expiresAt,
        List<QuoteItemResponseDTO> quoteItems
) {
}
