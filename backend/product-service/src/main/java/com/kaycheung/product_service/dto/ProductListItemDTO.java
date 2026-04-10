package com.kaycheung.product_service.dto;

import com.kaycheung.product_service.entity.StockAvailabilityStatus;

public record ProductListItemDTO(
        String id,
        String title,
        String priceString,
        String thumbnailUrl,
        String productCategory,
        boolean available,
        long availableStock,
        StockAvailabilityStatus availabilityStatus
) {
}
