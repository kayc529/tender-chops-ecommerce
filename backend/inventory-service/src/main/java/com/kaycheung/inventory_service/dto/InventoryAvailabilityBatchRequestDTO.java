package com.kaycheung.inventory_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityBatchRequestDTO(@NotEmpty List<@NotNull UUID> productIds) {
}
