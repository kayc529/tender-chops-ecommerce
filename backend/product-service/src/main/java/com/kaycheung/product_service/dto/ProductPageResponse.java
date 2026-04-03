package com.kaycheung.product_service.dto;


import java.util.List;

public record ProductPageResponse(
        List<ProductListItemDTO> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) { }
