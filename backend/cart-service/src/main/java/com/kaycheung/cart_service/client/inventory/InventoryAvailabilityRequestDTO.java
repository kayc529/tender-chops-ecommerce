package com.kaycheung.cart_service.client.inventory;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InventoryAvailabilityRequestDTO(@NotEmpty List<@NotNull UUID> productIds) {
}
