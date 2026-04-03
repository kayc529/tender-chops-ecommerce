package com.kaycheung.order_service.mapper;

import com.kaycheung.order_service.dto.QuoteItemResponseDTO;
import com.kaycheung.order_service.dto.QuoteResponseDTO;
import com.kaycheung.order_service.entity.Quote;
import com.kaycheung.order_service.entity.QuoteItem;
import com.kaycheung.order_service.util.PriceFormatUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = QuoteItemMapper.class)
public interface QuoteMapper {

    @Mapping(target = "quoteId", source = "quote.id")
    @Mapping(target = "quoteItems", source = "quoteItems")
    @Mapping(
            target = "totalAmount",
            expression = "java(calculateTotalAmount(quoteItems))"
    )
    @Mapping(
            target = "displayTotalAmount",
            expression = "java(formatTotalAmount(quoteItems))"
    )
    QuoteResponseDTO toDTO(Quote quote, List<QuoteItem> quoteItems);

    default Long calculateTotalAmount(List<QuoteItem> items) {
        return items.stream().mapToLong(QuoteItem::getLineTotalAmount).sum();
    }

    default String formatTotalAmount(List<QuoteItem> items) {
        long total = calculateTotalAmount(items);
        return PriceFormatUtils.formatPriceInCentsToDisplayString(total);
    }
}
