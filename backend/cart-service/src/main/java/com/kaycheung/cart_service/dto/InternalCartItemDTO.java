package com.kaycheung.cart_service.dto;

import java.util.UUID;

public record InternalCartItemDTO(
        UUID cartItemId,
        UUID productId,
        String productTitleSnapshot,
        String productDescriptionSnapshot,
        Long priceSnapshot,
        int quantity,
        Long lineTotalAmount) {
}
