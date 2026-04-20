package com.kaycheung.order_service.dto;

import java.util.UUID;

public record PublicOrderCreatePaymentResponseDTO(
        UUID paymentId,
        UUID paymentAttemptId,
        int attemptNo,
        String redirectUrl
) {
}
