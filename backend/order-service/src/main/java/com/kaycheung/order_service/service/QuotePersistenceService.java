package com.kaycheung.order_service.service;

import com.kaycheung.order_service.client.cart.CartItemResponseDTO;
import com.kaycheung.order_service.domain.PersistedQuoteAndQuoteItems;
import com.kaycheung.order_service.entity.Quote;
import com.kaycheung.order_service.entity.QuoteItem;
import com.kaycheung.order_service.mapper.QuoteItemMapper;
import com.kaycheung.order_service.repository.QuoteItemRepository;
import com.kaycheung.order_service.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuotePersistenceService {
    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;

    private final QuoteItemMapper quoteItemMapper;

    @Transactional
    public PersistedQuoteAndQuoteItems persistQuoteAndQuoteItems(Quote quote, List<CartItemResponseDTO> cartItems) {
        Quote savedQuote = quoteRepository.save(quote);
        UUID quoteId = savedQuote.getId();

        List<QuoteItem> quoteItems = quoteItemMapper.toQuoteItemList(quoteId, cartItems);

        List<QuoteItem> savedQuoteItems = quoteItemRepository.saveAll(quoteItems);

        return new PersistedQuoteAndQuoteItems(savedQuote, savedQuoteItems);
    }
}
