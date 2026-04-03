package com.kaycheung.order_service.domain;

import com.kaycheung.order_service.entity.Quote;
import com.kaycheung.order_service.entity.QuoteItem;

import java.util.List;

public record PersistedQuoteAndQuoteItems(Quote quote, List<QuoteItem> quoteItems) {
}
