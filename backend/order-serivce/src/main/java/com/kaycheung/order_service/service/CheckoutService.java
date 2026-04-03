package com.kaycheung.order_service.service;

import com.kaycheung.order_service.client.cart.CartClient;
import com.kaycheung.order_service.client.cart.CartItemResponseDTO;
import com.kaycheung.order_service.client.cart.CartResponseDTO;
import com.kaycheung.order_service.client.product.ProductClient;
import com.kaycheung.order_service.client.product.ProductDTO;
import com.kaycheung.order_service.client.product.ProductRequestDTO;
import com.kaycheung.order_service.client.product.ProductResponseDTO;
import com.kaycheung.order_service.domain.PersistedQuoteAndQuoteItems;
import com.kaycheung.order_service.dto.QuoteResponseDTO;
import com.kaycheung.order_service.entity.Quote;
import com.kaycheung.order_service.entity.QuoteItem;
import com.kaycheung.order_service.exception.domain.quote.QuoteInvalidException;
import com.kaycheung.order_service.exception.domain.quote.QuoteConflictException;
import com.kaycheung.order_service.exception.domain.quote.QuoteExpiredException;
import com.kaycheung.order_service.exception.domain.quote.QuoteNotFoundException;
import com.kaycheung.order_service.mapper.QuoteItemMapper;
import com.kaycheung.order_service.mapper.QuoteMapper;
import com.kaycheung.order_service.repository.QuoteItemRepository;
import com.kaycheung.order_service.repository.QuoteRepository;
import com.kaycheung.order_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuoteMapper quoteMapper;
    private final QuoteItemMapper quoteItemMapper;

    private final CartClient cartClient;
    private final ProductClient productClient;

    private final QuotePersistenceService quotePersistenceService;

    public QuoteResponseDTO getQuote(UUID quoteId) {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        Quote quote = quoteRepository.findByIdAndUserId(quoteId, userId).orElseThrow(() -> new QuoteNotFoundException(quoteId));

        Instant now = Instant.now();

        if (!quote.getExpiresAt().isAfter(now)) {
            throw new QuoteExpiredException("Quote expired at " + quote.getExpiresAt() + " (now=" + now + ")");
        }

        List<QuoteItem> quoteItems = quoteItemRepository.findByQuoteId(quote.getId());

        if (quoteItems.isEmpty()) {
            throw new QuoteInvalidException("Quote invalid: no items associated");
        }

        return quoteMapper.toDTO(quote, quoteItems);
    }

    //  v1 -> each user only has one cart
    //  future versions: fetch only the active cart
    //  and for existing, non-expired quote, check the sourceCartId against the active cart's id
    public QuoteResponseDTO createQuote() {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        //  get user's cart
        CartResponseDTO cartResponse = cartClient.getCart(userId);
        List<CartItemResponseDTO> cartItems = cartResponse.cartItems();

        if (cartItems.isEmpty()) {
            throw new QuoteConflictException("Cannot check out empty cart");
        }

        Map<UUID, CartItemResponseDTO> cartItemsByProductId = cartItems.stream().collect(Collectors.toMap(CartItemResponseDTO::productId, item -> item));

        ProductRequestDTO productRequest = new ProductRequestDTO(cartItemsByProductId.keySet().stream().toList());
        log.info("ProductRequest={}", productRequest);
        ProductResponseDTO productResponse = productClient.getProductsWithBasePrice(productRequest);

        //  if any product is missing
        if (!productResponse.missingProducts().isEmpty()) {
            throw new QuoteConflictException("Some products are no longer available");
        }

        Map<UUID, ProductDTO> productsByProductId = productResponse.products().stream().collect(Collectors.toMap(ProductDTO::productId, p -> p));
        long totalAmount = 0L;

        //  check for price conflict
        for (UUID productId : cartItemsByProductId.keySet()) {
            Long cartItemPriceSnapshot = cartItemsByProductId.get(productId).priceSnapshot();
            Long productPrice = productsByProductId.get(productId).basePrice();

            if (!cartItemPriceSnapshot.equals(productPrice)) {
                throw new QuoteConflictException("Price updated. Please refresh.");
            }

            totalAmount += cartItemPriceSnapshot * cartItemsByProductId.get(productId).quantity();
        }

        //  create quote and quote items
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(15));
        Quote quote = new Quote();
        quote.setUserId(userId);
        quote.setSourceCartId(cartResponse.cartId());
        quote.setSourceCartVersion(cartResponse.cartVersion());
        quote.setTotalAmount(totalAmount);
        quote.setCurrency("CAD");
        quote.setExpiresAt(expiresAt);

        //  persist quote and quote items
        PersistedQuoteAndQuoteItems persisted = quotePersistenceService.persistQuoteAndQuoteItems(quote, cartItemsByProductId.values().stream().toList());


        return quoteMapper.toDTO(persisted.quote(), persisted.quoteItems());
    }
}
