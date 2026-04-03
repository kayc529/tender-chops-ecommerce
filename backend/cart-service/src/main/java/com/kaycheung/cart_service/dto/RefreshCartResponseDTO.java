package com.kaycheung.cart_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RefreshCartResponseDTO(
        UUID cartId,
        List<RefreshCartItemResponseDTO> items,
        int totalQuantity,
        Long totalPrice,
        String displayTotalPrice,
        Instant createdAt,
        Instant updatedAt,
        Long cartVersion) {
}
