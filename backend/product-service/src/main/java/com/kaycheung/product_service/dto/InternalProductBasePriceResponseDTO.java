package com.kaycheung.product_service.dto;

import java.util.List;
import java.util.UUID;

public record InternalProductBasePriceResponseDTO(List<InternalProductBasePriceDTO> products, List<UUID> missingProducts) {
}
