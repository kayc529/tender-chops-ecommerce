package com.kaycheung.order_service.client.product;

import java.util.UUID;

public record ProductDTO(UUID productId, Long basePrice) {
}
