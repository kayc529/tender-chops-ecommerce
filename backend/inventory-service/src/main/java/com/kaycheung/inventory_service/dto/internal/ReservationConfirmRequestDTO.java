package com.kaycheung.inventory_service.dto.internal;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationConfirmRequestDTO(@NotNull UUID orderId, @NotNull UUID quoteId) {
}
