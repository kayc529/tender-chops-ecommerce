package com.kaycheung.cart_service.mapper;

import com.kaycheung.cart_service.dto.CartItemResponseDTO;
import com.kaycheung.cart_service.dto.InternalCartItemDTO;
import com.kaycheung.cart_service.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    //  entity to CartItemResponseDTO
    @Mapping(source = "id", target = "cartItemId")
    @Mapping(source = "productTitleSnapshot", target = "productTitle")
    @Mapping(source = "productDescriptionSnapshot", target = "productDescription")
    @Mapping(target = "displayPriceSnapshot", expression = "java(formatPrice(cartItem.getPriceSnapshot()))")
    CartItemResponseDTO toResponseDto(CartItem cartItem);

    @Mapping(source = "id", target = "cartItemId")
    @Mapping(target = "lineTotalAmount", expression = "java(cartItem.getQuantity() * cartItem.getPriceSnapshot())")
    InternalCartItemDTO toInternalResponseDTO(CartItem cartItem);

    List<InternalCartItemDTO> toInternalResponseDTOList(List<CartItem> cartItems);

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
