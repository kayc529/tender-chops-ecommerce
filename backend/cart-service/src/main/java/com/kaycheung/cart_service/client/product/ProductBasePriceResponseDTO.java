package com.kaycheung.cart_service.client.product;

import java.util.List;
import java.util.UUID;

public record ProductBasePriceResponseDTO(List<ProductBasePriceDTO> products, List<UUID> missingProducts) {
}
