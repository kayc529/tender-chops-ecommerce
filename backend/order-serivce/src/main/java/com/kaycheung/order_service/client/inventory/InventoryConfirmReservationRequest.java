package com.kaycheung.order_service.client.inventory;

import java.util.UUID;

public record InventoryConfirmReservationRequest(UUID orderId, UUID quoteId) {
}
