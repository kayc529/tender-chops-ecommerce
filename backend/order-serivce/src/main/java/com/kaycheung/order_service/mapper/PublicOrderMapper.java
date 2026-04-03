package com.kaycheung.order_service.mapper;

import com.kaycheung.order_service.dto.OrderItemResponseDTO;
import com.kaycheung.order_service.dto.PublicOrderResponseDTO;
import com.kaycheung.order_service.entity.Order;
import com.kaycheung.order_service.entity.OrderItem;
import com.kaycheung.order_service.util.PriceFormatUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = OrderItemMapper.class)
public interface PublicOrderMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "placedAt", source = "order.createdAt")
    @Mapping(target = "displayTotalAmount", expression = ("java(formatPriceToDisplayString(order.getTotalAmount()))"))
    @Mapping(target = "orderItems", source = "orderItems")
    PublicOrderResponseDTO toDto(Order order, List<OrderItem> orderItems);

    default String formatPriceToDisplayString(Long priceInCents)
    {
        if(priceInCents==null)
        {
            return null;
        }

        return PriceFormatUtils.formatPriceInCentsToDisplayString(priceInCents);
    }
}
