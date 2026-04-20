package com.kaycheung.order_service.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicOrderCreateRequestDTOAddress(
        @NotBlank String receiver,
        @NotBlank String phone,
        @NotBlank String addressLine1,
        String addressLine2,
        @NotBlank String city,
        @NotBlank String stateOrProvince,
        @NotBlank String postalCode,
        @NotBlank String country
) {
}
