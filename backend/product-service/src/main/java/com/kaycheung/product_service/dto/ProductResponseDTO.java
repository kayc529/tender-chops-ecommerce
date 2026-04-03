package com.kaycheung.product_service.dto;

public record ProductResponseDTO(
        String id,
        String title,
        String description,
        String portionDescription,
        Long basePrice,
        String priceString,
        String imageUrl,
        String thumbnailUrl,
        String productCategory) {
}
