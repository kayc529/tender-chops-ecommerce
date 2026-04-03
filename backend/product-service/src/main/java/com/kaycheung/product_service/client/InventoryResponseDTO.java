package com.kaycheung.product_service.client;

import java.util.UUID;

public record InventoryResponseDTO(UUID productId, int availableQuantity) {
}
