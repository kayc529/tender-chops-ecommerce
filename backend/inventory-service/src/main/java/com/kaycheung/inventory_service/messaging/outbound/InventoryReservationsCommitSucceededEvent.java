package com.kaycheung.inventory_service.messaging.outbound;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationsCommitSucceededEvent(
        UUID eventId,
        Instant occurredAt,
        UUID quoteId,
        UUID orderId,
        UUID paymentId
) {
}
