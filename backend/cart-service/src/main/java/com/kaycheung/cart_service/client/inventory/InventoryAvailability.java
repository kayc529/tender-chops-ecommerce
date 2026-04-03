package com.kaycheung.cart_service.client.inventory;

import java.util.UUID;

public record InventoryAvailability(UUID productId, int availableQuantity) {
}
