package com.kaycheung.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaycheung.order_service.client.inventory.*;
import com.kaycheung.order_service.client.payment.CreatePaymentRequest;
import com.kaycheung.order_service.client.payment.CreatePaymentResponse;
import com.kaycheung.order_service.client.payment.PaymentClient;
import com.kaycheung.order_service.client.product.ProductClient;
import com.kaycheung.order_service.client.product.ProductDTO;
import com.kaycheung.order_service.client.product.ProductRequestDTO;
import com.kaycheung.order_service.client.product.ProductResponseDTO;
import com.kaycheung.order_service.domain.FilterDateRange;
import com.kaycheung.order_service.domain.TimeFilter;
import com.kaycheung.order_service.dto.*;
import com.kaycheung.order_service.entity.*;
import com.kaycheung.order_service.event.OrderCanceledEvent;
import com.kaycheung.order_service.exception.client.PaymentClientException;
import com.kaycheung.order_service.exception.domain.order.OrderChangeStatusException;
import com.kaycheung.order_service.exception.domain.order.OrderNotFoundException;
import com.kaycheung.order_service.exception.domain.order.OrderPersistenceFailedException;
import com.kaycheung.order_service.exception.domain.order.OrderUnauthorizedAccessException;
import com.kaycheung.order_service.exception.domain.quote.*;
import com.kaycheung.order_service.mapper.PublicOrderMapper;
import com.kaycheung.order_service.messaging.outbox.OutboxEventService;
import com.kaycheung.order_service.messaging.outbox.OutboxEventType;
import com.kaycheung.order_service.messaging.outbox.payload.OrderCanceledPayload;
import com.kaycheung.order_service.messaging.outbox.payload.OrderCreatedPayload;
import com.kaycheung.order_service.messaging.outbox.payload.OrderCreationFailedPayload;
import com.kaycheung.order_service.repository.OrderItemRepository;
import com.kaycheung.order_service.repository.OrderRepository;
import com.kaycheung.order_service.repository.QuoteItemRepository;
import com.kaycheung.order_service.repository.QuoteRepository;
import com.kaycheung.order_service.util.ObjectMapperUtils;
import com.kaycheung.order_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicOrderService {
    private final OrderPersistenceService orderPersistenceService;
    private final InternalOrderService internalOrderService;
    private final OutboxEventService outboxEventService;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository quoteItemRepository;

    private final PublicOrderMapper orderMapper;

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final ObjectMapperUtils objectMapperUtils;


    public PublicOrderResponseDTO getOrder(UUID orderId) {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderUnauthorizedAccessException();
        }

        //  get order items with orderId
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        return orderMapper.toDto(order, orderItems);
    }

    public OrderPageResponseDTO getOrders(
            String timeFilterParam,
            LocalDate start,
            LocalDate end,
            Pageable pageable
    ) {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        Page<Order> orders;

        //  get filter type & date range to pass to repo
        TimeFilter filter = TimeFilter.fromParam(timeFilterParam);
        FilterDateRange dateRange = filter.toDateRange(start, end);

        //  filter = all
        if (dateRange.isUnbounded()) {
            orders = orderRepository.findByUserId(userId, pageable);
        } else {
            orders = orderRepository.findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(userId, dateRange.startInclusive(), dateRange.endExclusive(), pageable);
        }

        List<UUID> orderIds = orders.getContent().stream().map(Order::getId).toList();
        Map<UUID, List<OrderItem>> itemsByOrderId = new HashMap<>();

        if (!orderIds.isEmpty()) {
            List<OrderItem> batchItems = orderItemRepository.findByOrderIdIn(orderIds);

            for (OrderItem item : batchItems) {
                itemsByOrderId.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
            }
        }


        List<PublicOrderResponseDTO> orderDtos = orders.getContent().stream()
                .map(order -> {
                    List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), List.of());
                    return orderMapper.toDto(order, orderItems);
                })
                .toList();

        return new OrderPageResponseDTO(
                orderDtos,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isFirst(),
                orders.isLast(),
                orders.hasNext(),
                orders.hasPrevious()
        );
    }


    public PublicOrderCreateResponseDTO createOrder(PublicOrderCreateRequestDTO request) {

        UUID userId = SecurityUtils.getCurrentUserIdUUID();
        Quote quote = quoteRepository.findById(request.quoteId()).orElseThrow(() -> new QuoteNotFoundException(request.quoteId()));

        //  UnauthorizedQuoteAccessException - userId of quote differs from current userId
        if (!userId.equals(quote.getUserId())) {
            throw new QuoteUnauthorizedAccessException();
        }

        //  InvalidQuoteException - quote is expired
        Instant now = Instant.now();
        if (!quote.getExpiresAt().isAfter(now)) {
            throw new QuoteExpiredException("Quote expired at " + quote.getExpiresAt() + " (now=" + now + ")");
        }

        //  get quote items with quoteId
        List<QuoteItem> quoteItems = quoteItemRepository.findByQuoteId(request.quoteId());

        //  InvalidQuoteException - quote item list is empty
        if (quoteItems.isEmpty()) {
            throw new QuoteInvalidException("Quote invalid: no items associated");
        }

        //  guardrail - check if order has already been placed with this quote
        //  return existing order
        Order existingOrder = orderRepository.findByUserIdAndSourceQuoteId(userId, quote.getId()).orElse(null);

        if (existingOrder != null) {
            // Return the existing order, but also return the existing attempt #1 payment redirectUrl
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(existingOrder.getId());
            PublicOrderResponseDTO orderDto = orderMapper.toDto(existingOrder, orderItems);

            //  idempotency allows payment-service to return the existing payment
            CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest(
                    orderDto.orderId(),
                    orderDto.totalAmount(),
                    orderDto.currency()
            );
            //  get the existing payment - API call
            CreatePaymentResponse createPaymentResponse = paymentClient.createPayment(createPaymentRequest, userId);

            PublicOrderCreatePaymentResponseDTO paymentDto = new PublicOrderCreatePaymentResponseDTO(
                    createPaymentResponse.paymentId(),
                    createPaymentResponse.paymentAttemptId(),
                    createPaymentResponse.attemptNo(),
                    createPaymentResponse.redirectUrl()
            );

            // don't re-reserve inventory here, return the quote expiry as the best available time bound.
            return new PublicOrderCreateResponseDTO(orderDto, paymentDto, quote.getExpiresAt());
        }

        Map<UUID, QuoteItem> quoteItemsByProductId = quoteItems.stream().collect(Collectors.toMap(
                QuoteItem::getProductId, q -> q
        ));
        List<UUID> productIds = quoteItemsByProductId.keySet().stream().toList();

        //  get the list of product prices from product-service - API call
        ProductRequestDTO productRequest = new ProductRequestDTO(productIds);
        ProductResponseDTO productResponse = productClient.getProductsWithBasePrice(productRequest);

        //  QuoteConflictException -  missing products
        if (!productResponse.missingProducts().isEmpty()) {
            throw new QuoteConflictException("Some products are no longer available");
        }


        Map<UUID, ProductDTO> productsByProductId = productResponse.products().stream()
                .collect(Collectors.toMap(ProductDTO::productId, p -> p
                ));

        //  QuoteConflictException - number of entries between quoteItems and products differ
        if (productsByProductId.keySet().size() != quoteItemsByProductId.keySet().size()) {
            throw new QuoteConflictException("Product mismatch");
        }

        Long totalAmountCalculated = 0L;

        //  price matching
        for (UUID productId : productsByProductId.keySet()) {
            Long productBasePrice = productsByProductId.get(productId).basePrice();
            Long quotePriceSnapshot = quoteItemsByProductId.get(productId).getUnitPriceSnapshot();
            int quantity = quoteItemsByProductId.get(productId).getQuantity();

            //  QuoteConflictException - any price entry difference
            if (!productBasePrice.equals(quotePriceSnapshot)) {
                throw new QuoteConflictException("Product price changed since quote was created");
            }

            totalAmountCalculated += quotePriceSnapshot * quantity;
        }

        //  compute totalAmounts of quote items
        //  QuoteConflictException - totalAmounts (recomputed total and quote total) different
        if (!totalAmountCalculated.equals(quote.getTotalAmount())) {
            throw new QuoteConflictException("Quote total amount is inconsistent with line totals");
        }

        //  check stock and create reservations in inventory-service - API call
        List<InventoryRequestItemDTO> itemsToReserve = quoteItemsByProductId.values().stream().map(quoteItem -> new InventoryRequestItemDTO(quoteItem.getProductId(), quoteItem.getQuantity())).toList();
        InventoryRequestDTO inventoryRequest = new InventoryRequestDTO(request.quoteId(), itemsToReserve);
        InventoryResponseDTO inventoryResponse = inventoryClient.checkStockAndCreateReservations(inventoryRequest);

        //  create order and order Items
        PublicOrderResponseDTO orderDto;
        try {
            //  emptyCart is called when the transaction is committed
            orderDto = orderPersistenceService.createOrderAndOrderItems(userId, request.address(), quote, quoteItemsByProductId);
        } catch (DataIntegrityViolationException ex) {
            //  Race: concurrent createOrder for the same (userId, sourceQuoteId) —
            //  treat as idempotent: refetch the existing order and continue.
            Order existing = orderRepository.findByUserIdAndSourceQuoteId(userId, quote.getId()).orElse(null);
            if (existing == null) {
                throw new OrderPersistenceFailedException("Order already exists but cannot be refetched. Please try again.");
            }

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(existing.getId());
            orderDto = orderMapper.toDto(existing, orderItems);
        } catch (Exception ex) {
            //  best-effort compensation
//            InventoryReleaseReservationRequestDTO releaseRequest = new InventoryReleaseReservationRequestDTO(request.quoteId());
//            inventoryClient.releaseReservationsForCompensation(releaseRequest);
            OrderCreationFailedPayload payloadObject = new OrderCreationFailedPayload(quote.getId());
            String payload = objectMapperUtils.toJson(payloadObject);
            String key = "order:quote:" + quote.getId() + "order_creation_failed";
            outboxEventService.createOutboxEvent(OutboxEventType.ORDER_CREATION_FAILED, payload, key);

            throw new OrderPersistenceFailedException("Failed to create order. Please try again.");
        }

        //  create payment - API call
        CreatePaymentRequest createPaymentRequest = new CreatePaymentRequest(orderDto.orderId(), orderDto.totalAmount(), orderDto.currency());
        CreatePaymentResponse createPaymentResponse;

        try {
            createPaymentResponse = paymentClient.createPayment(createPaymentRequest, userId);
        } catch (PaymentClientException ex) {
            try {
                internalOrderService.cancelOrder(orderDto.orderId());
            } catch (Exception ignored) {
                //  best-effort
            }
            throw ex;
        }

        PublicOrderCreatePaymentResponseDTO paymentDto = new PublicOrderCreatePaymentResponseDTO(createPaymentResponse.paymentId(), createPaymentResponse.paymentAttemptId(), createPaymentResponse.attemptNo(), createPaymentResponse.redirectUrl());

        return new PublicOrderCreateResponseDTO(orderDto, paymentDto, inventoryResponse.expiresAt());
    }

    @Transactional
    public PublicOrderResponseDTO cancelOrder(UUID orderId) {
        UUID userId = SecurityUtils.getCurrentUserIdUUID();

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!userId.equals(order.getUserId())) {
            throw new OrderUnauthorizedAccessException();
        }

        //  v1 only allows user to cancel order when it's still pending payment
        //  only allows user to cancel paid order when refund service is implemented
        int updated = orderPersistenceService.markCanceledIfCancelable(orderId);

        if (updated == 0) {
            Order latest = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));

            throw new OrderChangeStatusException(
                    "Failed to cancel order because of status: " + latest.getOrderStatus()
            );
        }

        //  for DTO
        order.setOrderStatus(OrderStatus.CANCELED);

        eventPublisher.publishEvent(new OrderCanceledEvent(order.getSourceQuoteId()));

        //  create ORDER_CANCELED outbox event
        String key = "order:" + orderId + ":canceled";
        String payload = buildOutboxPayloadForCancelOrder(order);
        outboxEventService.createOutboxEvent(OutboxEventType.ORDER_CANCELED, payload, key);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        return orderMapper.toDto(order, orderItems);
    }

    private String buildOutboxPayloadForCancelOrder(Order order) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("orderId", order.getId().toString());
        node.put("quoteId", order.getSourceQuoteId().toString());

        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload (ORDER_CANCELED) for orderId=" + order.getId(), e);
        }
    }


}
