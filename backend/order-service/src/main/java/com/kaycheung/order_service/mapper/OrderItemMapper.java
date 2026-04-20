package com.kaycheung.order_service.mapper;

import com.kaycheung.order_service.dto.OrderItemResponseDTO;
import com.kaycheung.order_service.entity.OrderItem;
import com.kaycheung.order_service.util.PriceFormatUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "orderItemId", source = "id")
    @Mapping(target = "productTitle", source = "productTitleSnapshot")
    @Mapping(target = "unitPrice", source = "unitPriceSnapshot")
    @Mapping(target = "displayUnitPrice", expression = "java(formatPriceToDisplayString(orderItem.getUnitPriceSnapshot()))")
    @Mapping(target = "displayLineTotalAmount", expression = "java(formatPriceToDisplayString(orderItem.getLineTotalAmount()))")
    OrderItemResponseDTO toDto(OrderItem orderItem);

    List<OrderItemResponseDTO> toDTOList(List<OrderItem> orderItems);

    default String formatPriceToDisplayString(Long priceInCents) {
        if (priceInCents == null) {
            return null;
        }

        return PriceFormatUtils.formatPriceInCentsToDisplayString(priceInCents);
    }
}
