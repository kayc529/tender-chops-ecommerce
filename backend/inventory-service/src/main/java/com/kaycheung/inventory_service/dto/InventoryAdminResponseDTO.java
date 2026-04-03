package com.kaycheung.inventory_service.dto;

import java.util.UUID;

//  share with internal (services)
public record InventoryAdminResponseDTO(UUID id, UUID productId, int totalQuantity, int reservedQuantity, int availableQuantity) {
}
