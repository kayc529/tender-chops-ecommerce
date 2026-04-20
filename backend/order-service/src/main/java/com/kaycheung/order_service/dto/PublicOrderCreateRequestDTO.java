package com.kaycheung.order_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PublicOrderCreateRequestDTO(
        @NotNull UUID quoteId,
        @NotNull PublicOrderCreateRequestDTOAddress address) {
}
