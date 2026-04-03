package com.kaycheung.order_service.dto;

import java.time.Instant;

public record PublicOrderCreateResponseDTO(
        PublicOrderResponseDTO order,
        PublicOrderCreatePaymentResponseDTO payment,
        Instant reservationExpiresAt
) {
}
