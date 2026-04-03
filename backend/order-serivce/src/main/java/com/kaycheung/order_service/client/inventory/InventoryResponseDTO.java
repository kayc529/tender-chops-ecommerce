package com.kaycheung.order_service.client.inventory;

import java.time.Instant;

public record InventoryResponseDTO(Instant expiresAt) {
}
