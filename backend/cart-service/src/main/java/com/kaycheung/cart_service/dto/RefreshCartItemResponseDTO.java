package com.kaycheung.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RefreshCartItemResponseDTO {

    UUID cartItemId;
    UUID productId;
    String productTitle;
    String productDescription;
    Long priceSnapshot;
    String displayPriceSnapshot;
    int quantity;
    String oldDisplayPrice;
    boolean priceChanged;
    boolean available;
    int availableQuantity;
}
