package com.kaycheung.order_service.client.inventory;

import java.util.UUID;

public record InventoryRequestItemDTO(UUID productId, int quantity) {
}
