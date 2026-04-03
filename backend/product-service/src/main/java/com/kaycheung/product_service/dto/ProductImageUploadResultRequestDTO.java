package com.kaycheung.product_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProductImageUploadResultRequestDTO(
        @NotNull UUID productId,
        @NotNull String imageKey,
        @NotNull String thumbnailKey,
        @NotNull boolean success,
        String failureReason
) {
}
