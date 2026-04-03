package com.kaycheung.inventory_service.messaging.inbound;

import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID eventId,
        Instant occurredAt,

        UUID quoteId,
        UUID orderId,
        UUID paymentId,

        String currency,
        Long totalAmount) {
}
