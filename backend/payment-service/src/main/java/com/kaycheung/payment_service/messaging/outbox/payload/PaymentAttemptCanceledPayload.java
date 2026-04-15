package com.kaycheung.payment_service.messaging.outbox.payload;

import java.util.UUID;

public record PaymentAttemptCanceledPayload(
        UUID paymentId,
        UUID paymentAttemptId,
        UUID orderId
) {
}
