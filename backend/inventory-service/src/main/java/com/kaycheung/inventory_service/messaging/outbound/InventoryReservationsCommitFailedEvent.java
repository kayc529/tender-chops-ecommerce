package com.kaycheung.inventory_service.messaging.outbound;

import com.kaycheung.inventory_service.messaging.contract.InventoryEventReasonCode;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservationsCommitFailedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID quoteId,
        UUID orderId,
        UUID paymentId,
        InventoryEventReasonCode reasonCode,
        String message
) {
}
