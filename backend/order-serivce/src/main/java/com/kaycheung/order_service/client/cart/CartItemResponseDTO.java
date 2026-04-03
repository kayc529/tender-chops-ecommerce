package com.kaycheung.order_service.client.cart;

import java.util.UUID;

public record CartItemResponseDTO(
        UUID cartItemId,
        UUID productId,
        String productTitleSnapshot,
        Long priceSnapshot,
        int quantity) {
}
