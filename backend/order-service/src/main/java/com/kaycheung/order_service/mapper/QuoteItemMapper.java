package com.kaycheung.order_service.mapper;

import com.kaycheung.order_service.client.cart.CartItemResponseDTO;
import com.kaycheung.order_service.dto.QuoteItemResponseDTO;
import com.kaycheung.order_service.entity.QuoteItem;
import com.kaycheung.order_service.util.PriceFormatUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface QuoteItemMapper {
    @Mapping(target = "quoteItemId", source = "id")
    @Mapping(target = "productTitle", source = "productTitleSnapshot")
    @Mapping(target = "unitPrice", source = "unitPriceSnapshot")
    @Mapping(target = "displayUnitPrice", expression = "java(formatPriceToDisplayString(quoteItem.getUnitPriceSnapshot()))")
    @Mapping(target = "displayLineTotalAmount", expression = "java(formatPriceToDisplayString(quoteItem.getLineTotalAmount()))")
    QuoteItemResponseDTO toDto(QuoteItem quoteItem);

    List<QuoteItemResponseDTO> toDtoList(List<QuoteItem> quoteItems);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "quoteId", source = "quoteId")
    @Mapping(target = "unitPriceSnapshot", source = "cartItem.priceSnapshot")
    @Mapping(target = "lineTotalAmount", expression = "java(cartItem.priceSnapshot() * cartItem.quantity())")
    QuoteItem toQuoteItem(UUID quoteId, CartItemResponseDTO cartItem);

//    List<QuoteItem> toQuoteItemList(UUID quoteId, List<CartItemResponseDTO> cartItems);

    default String formatPriceToDisplayString(Long priceInCents) {
        if (priceInCents == null) {
            return null;
        }

        return PriceFormatUtils.formatPriceInCentsToDisplayString(priceInCents);
    }

    default List<QuoteItem> toQuoteItemList(UUID quoteId, List<CartItemResponseDTO> cartItems){
        if(cartItems == null)
        {
            return List.of();
        }

        return cartItems.stream().map(item->toQuoteItem(quoteId, item))
                .toList();
    }
}
