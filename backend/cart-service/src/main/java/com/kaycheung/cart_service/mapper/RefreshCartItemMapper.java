package com.kaycheung.cart_service.mapper;

import com.kaycheung.cart_service.dto.RefreshCartItemResponseDTO;
import com.kaycheung.cart_service.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface RefreshCartItemMapper {

    @Mapping(source = "id", target = "cartItemId")
    @Mapping(source = "productTitleSnapshot", target = "productTitle")
    @Mapping(source = "productDescriptionSnapshot", target = "productDescription")
    @Mapping(target = "displayPriceSnapshot", expression = "java(formatPrice(cartItem.getPriceSnapshot()))")
    @Mapping(target = "oldDisplayPrice", ignore = true)
    @Mapping(target = "priceChanged", constant = "false")
    @Mapping(target = "available", constant = "true")
    @Mapping(target = "availableQuantity", constant = "0")
    RefreshCartItemResponseDTO toDto(CartItem cartItem);

    default String formatPrice(Long priceSnapshot)
    {
        if(priceSnapshot==null)
        {
            return null;
        }
        BigDecimal price = BigDecimal.valueOf(priceSnapshot,2);
        return price.toPlainString();
    }
}
