package com.kaycheung.inventory_service.dto.internal;

import java.time.Instant;

public record ReservationCreateResponseDTO(Instant expiresAt) {
}
