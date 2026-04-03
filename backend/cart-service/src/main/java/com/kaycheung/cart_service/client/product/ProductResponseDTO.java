package com.kaycheung.cart_service.client.product;

import java.util.UUID;

public record ProductResponseDTO(
        UUID id,
        String title,
        String description,
        Long basePrice
) {
}
