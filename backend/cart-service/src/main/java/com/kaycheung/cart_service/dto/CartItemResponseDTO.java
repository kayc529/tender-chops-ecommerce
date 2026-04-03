package com.kaycheung.cart_service.dto;

import java.util.UUID;

public record CartItemResponseDTO(
        UUID cartItemId,
        UUID productId,
        String productTitle,
        String productDescription,
        Long priceSnapshot,
        String displayPriceSnapshot,
        int quantity
) {
}
