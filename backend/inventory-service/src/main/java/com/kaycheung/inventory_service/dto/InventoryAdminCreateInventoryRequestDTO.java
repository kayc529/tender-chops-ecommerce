package com.kaycheung.inventory_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryAdminCreateInventoryRequestDTO(@NotNull UUID productId) {
}
