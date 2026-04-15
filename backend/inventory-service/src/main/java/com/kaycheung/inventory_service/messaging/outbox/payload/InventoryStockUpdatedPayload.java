package com.kaycheung.inventory_service.messaging.outbox.payload;

import java.util.UUID;

public record InventoryStockUpdatedPayload(
        UUID productId,
        int availableStock,
        String availabilityStatus,
        long stockVersion
) {
}
