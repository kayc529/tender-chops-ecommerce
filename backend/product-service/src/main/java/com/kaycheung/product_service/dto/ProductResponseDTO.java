package com.kaycheung.product_service.dto;

import com.kaycheung.product_service.entity.StockAvailabilityStatus;

public record ProductResponseDTO(
        String id,
        String title,
        String description,
        String portionDescription,
        Long basePrice,
        String priceString,
        String imageUrl,
        String thumbnailUrl,
        String productCategory,
        boolean available,
        long availableStock,
        StockAvailabilityStatus availabilityStatus

) {
}
