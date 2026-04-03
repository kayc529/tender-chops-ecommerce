package com.kaycheung.cart_service.dto;

import java.util.List;

public record MergeGuestCartRequestDTO(
        List<MergeGuestCartItemRequestDTO> cartItems
) {
}
