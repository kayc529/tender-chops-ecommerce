package com.kaycheung.inventory_service.dto;

import java.util.UUID;

public record InventoryAvailabilityResponseDTO(UUID productId, int availableQuantity) {
}
