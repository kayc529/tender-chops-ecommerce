package com.kaycheung.payment_service.dto;

import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentId,
        UUID paymentAttemptId,
        UUID orderId,
        int attemptNo,
        String redirectUrl,
        String checkoutSessionId
) {
}
