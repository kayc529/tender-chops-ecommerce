package com.kaycheung.product_service.dto;

public record UploadProductImageResponseDTO(
        String uploadUrl,
        String expectedContentType
) {
}
