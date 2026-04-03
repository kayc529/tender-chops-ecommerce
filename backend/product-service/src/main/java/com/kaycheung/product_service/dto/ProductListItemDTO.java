package com.kaycheung.product_service.dto;

public record ProductListItemDTO(
        String id,
        String title,
        String priceString,
        String thumbnailUrl,
        String productCategory
) {
}
