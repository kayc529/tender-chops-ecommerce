package com.kaycheung.inventory_service.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReservationCreateRequestItem(
        @NotNull UUID productId,
        @Positive int quantity
) {
}
