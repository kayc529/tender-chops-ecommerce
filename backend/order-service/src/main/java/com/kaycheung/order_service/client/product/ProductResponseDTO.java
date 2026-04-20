package com.kaycheung.order_service.client.product;

import java.util.List;
import java.util.UUID;

public record ProductResponseDTO(List<ProductDTO> products, List<UUID> missingProducts) {
}
