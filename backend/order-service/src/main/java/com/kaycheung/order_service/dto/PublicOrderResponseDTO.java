package com.kaycheung.order_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicOrderResponseDTO(
        UUID orderId,
        String orderStatus,
        Instant placedAt,
        String currency,
        Long totalAmount,
        String displayTotalAmount,
        String receiver,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String stateOrProvince,
        String postalCode,
        String country,
        List<OrderItemResponseDTO> orderItems
        ) {
}
