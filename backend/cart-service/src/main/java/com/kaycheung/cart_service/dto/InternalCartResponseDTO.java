package com.kaycheung.cart_service.dto;

import java.util.List;
import java.util.UUID;

public record InternalCartResponseDTO(
        UUID cartId,
        Long cartVersion,
        List<InternalCartItemDTO> cartItems) {
}
