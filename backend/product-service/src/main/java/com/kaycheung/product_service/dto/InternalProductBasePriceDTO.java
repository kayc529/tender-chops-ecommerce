package com.kaycheung.product_service.dto;

import java.util.UUID;

public record InternalProductBasePriceDTO(UUID productId, Long basePrice) {
}
