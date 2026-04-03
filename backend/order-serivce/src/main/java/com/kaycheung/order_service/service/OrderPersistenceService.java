package com.kaycheung.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaycheung.order_service.dto.PublicOrderCreateRequestDTOAddress;
import com.kaycheung.order_service.dto.PublicOrderResponseDTO;
import com.kaycheung.order_service.entity.*;
import com.kaycheung.order_service.event.OrderCreatedEvent;
import com.kaycheung.order_service.exception.domain.order.OrderNotFoundException;
import com.kaycheung.order_service.mapper.PublicOrderMapper;
import com.kaycheung.order_service.messaging.inbox.handler.PaymentInboxEventHandler;
import com.kaycheung.order_service.messaging.outbox.OutboxEventService;
import com.kaycheung.order_service.messaging.outbox.OutboxEventType;
import com.kaycheung.order_service.repository.OrderItemRepository;
import com.kaycheung.order_service.repository.OrderRepository;
import com.kaycheung.order_service.repository.projection.OrderSourceQuoteIdProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPersistenceService {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PublicOrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxEventService outboxEventService;

    @Transactional
    public PublicOrderResponseDTO createOrderAndOrderItems(UUID userId, PublicOrderCreateRequestDTOAddress address, Quote quote, Map<UUID, QuoteItem> quoteItemsByProductId) {
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setSourceQuoteId(quote.getId());
        newOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        newOrder.setCurrency("CAD");
        newOrder.setTotalAmount(quote.getTotalAmount());
        mapOrderAddress(newOrder, address);

        Order orderSaved = orderRepository.save(newOrder);

        List<OrderItem> orderItemsToSave = new ArrayList<>();
        for (QuoteItem quoteItem : quoteItemsByProductId.values()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderSaved.getId());
            orderItem.setProductId(quoteItem.getProductId());
            orderItem.setProductTitleSnapshot(quoteItem.getProductTitleSnapshot());
            orderItem.setUnitPriceSnapshot(quoteItem.getUnitPriceSnapshot());
            orderItem.setQuantity(quoteItem.getQuantity());
            orderItem.setLineTotalAmount(quoteItem.getUnitPriceSnapshot() * quoteItem.getQuantity());

            orderItemsToSave.add(orderItem);
        }
        List<OrderItem> orderItemsSaved = orderItemRepository.saveAll(orderItemsToSave);

        //  TODO change to AWS SNS
        //  publish event to empty cart
        eventPublisher.publishEvent(new OrderCreatedEvent(userId));

        return orderMapper.toDto(orderSaved, orderItemsSaved);
    }

    public OrderSourceQuoteIdProjection getOrderSourceQuoteId(UUID orderId) {
        return orderRepository.findProjectedById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional
    public void markProcessingAndEnqueueReadyToCapture(UUID inboxEventId, PaymentInboxEventHandler.PaymentEventPayload p) {
        List<OrderStatus> validStatuses = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);
        int updated = orderRepository.transitionStatusIfIn(p.orderId(), OrderStatus.PROCESSING, validStatuses);

        if (updated == 0) {
            // Someone else already moved it (cancel/expired/etc). Do NOT publish capture-ready.
            log.warn("Skip ORDER_READY_TO_CAPTURE: order not in payable state anymore. orderId={} inboxEventId={}",
                    p.orderId(), inboxEventId);
            return;
        }

        // 3) Publish ORDER_READY_TO_CAPTURE (idempotent on orderId)
        String payload = buildOrderOutboxPayload(p, null);
        String key = "order:" + p.orderId() + ":ready_to_capture";
        outboxEventService.createOutboxEvent(OutboxEventType.ORDER_READY_TO_CAPTURE, payload, key);
    }

    @Transactional
    public void markExpiredAndEnqueueDoNotCapture(UUID inboxEventId, PaymentInboxEventHandler.PaymentEventPayload p, String reason) {
        List<OrderStatus> validStatuses = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);
        int updated = orderRepository.transitionStatusIfIn(p.orderId(), OrderStatus.EXPIRED, validStatuses);

        if (updated == 0) {
            log.warn("Skip ORDER_DO_NOT_CAPTURE: order not in payable state anymore. orderId={} inboxEventId={}",
                    p.orderId(), inboxEventId);
            return;
        }

        // 2) Publish ORDER_DO_NOT_CAPTURE (idempotent on orderId)
        String payload = buildOrderOutboxPayload(p, reason);
        String key = "order:" + p.orderId() + ":do_not_capture";
        outboxEventService.createOutboxEvent(OutboxEventType.ORDER_DO_NOT_CAPTURE, payload, key);
    }

    @Transactional
    public int markPaymentFailedIfPayable(UUID orderId) {
        //  PAYMENT_FAILED -> PAYMENT_FAILED counts as updated
        List<OrderStatus> validStatuses = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);
        return orderRepository.transitionStatusIfIn(orderId, OrderStatus.PAYMENT_FAILED, validStatuses);
    }

    @Transactional
    public int markPaidIfProcessing(UUID orderId) {
        return orderRepository.transitionStatusIfIn(orderId, OrderStatus.PAID, List.of(OrderStatus.PROCESSING));
    }

    @Transactional
    public int markCanceledIfCancelable(UUID orderId) {
        List<OrderStatus> validStatuses = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAYMENT_FAILED);
        return orderRepository.transitionStatusIfIn(orderId, OrderStatus.CANCELED, validStatuses);
    }

    private void mapOrderAddress(Order newOrder, PublicOrderCreateRequestDTOAddress requestAddress) {
        newOrder.setReceiver(requestAddress.receiver());
        newOrder.setPhone(requestAddress.phone());
        newOrder.setAddressLine1(requestAddress.addressLine1());
        newOrder.setAddressLine2(requestAddress.addressLine2());
        newOrder.setCity(requestAddress.city());
        newOrder.setStateOrProvince(requestAddress.stateOrProvince());
        newOrder.setPostalCode(requestAddress.postalCode());
        newOrder.setCountry(requestAddress.country());
    }

    private String buildOrderOutboxPayload(PaymentInboxEventHandler.PaymentEventPayload p, String reason) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("orderId", p.orderId().toString());
        node.put("paymentId", p.paymentId().toString());
        node.put("paymentAttemptId", p.paymentAttemptId().toString());

        if (reason != null && !reason.isBlank()) {
            node.put("reason", reason);
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload for orderId=" + p.orderId(), e);
        }
    }
}
