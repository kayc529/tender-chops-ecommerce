package com.kaycheung.order_service.client.payment;

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
