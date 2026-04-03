package com.kaycheung.inventory_service.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record InventoryAdminUpdateInventoryRequestDTO(@PositiveOrZero int totalQuantity) {
}
