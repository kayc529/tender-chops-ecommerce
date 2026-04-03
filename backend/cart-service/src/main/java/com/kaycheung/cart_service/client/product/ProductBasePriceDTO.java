package com.kaycheung.cart_service.client.product;

import java.util.UUID;

public record ProductBasePriceDTO(UUID productId, Long basePrice) {
}
