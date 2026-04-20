package com.kaycheung.order_service.client.inventory;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record InventoryRequestDTO(@NotNull UUID quoteId, @NotEmpty List<@NotNull InventoryRequestItemDTO> itemsToReserve) {
}
