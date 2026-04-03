package com.kaycheung.inventory_service.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReservationCreateRequestDTO(
        @NotNull UUID quoteId,
        @NotEmpty List<ReservationCreateRequestItem> itemsToReserve
) {
}
