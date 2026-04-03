package com.kaycheung.product_service.dto;

import jakarta.validation.constraints.NotNull;

public record UploadProductImageRequestDTO(
    @NotNull String contentType,
    @NotNull String originalFilename
) {
}
