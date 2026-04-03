package com.kaycheung.order_service.dto;

import java.util.List;

public record OrderPageResponseDTO(
        List<PublicOrderResponseDTO> orders,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious) {
}
