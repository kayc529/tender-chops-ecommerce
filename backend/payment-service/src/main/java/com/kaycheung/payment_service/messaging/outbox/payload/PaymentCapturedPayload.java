package com.kaycheung.payment_service.messaging.outbox.payload;

import java.util.UUID;

public record PaymentCapturedPayload(
        UUID paymentId,
        UUID paymentAttemptId,
        UUID orderId
) {
}
