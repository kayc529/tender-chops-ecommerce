package com.kaycheung.order_service.client.inventory;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryReleaseReservationRequestDTO(@NotNull UUID quoteId) {
}
