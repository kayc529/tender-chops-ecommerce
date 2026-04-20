package com.kaycheung.order_service.service;

import com.kaycheung.order_service.dto.internal.InternalUpdateOrderStatusDTO;
import com.kaycheung.order_service.entity.Order;
import com.kaycheung.order_service.entity.OrderStatus;
import com.kaycheung.order_service.event.OrderCanceledEvent;
import com.kaycheung.order_service.exception.domain.order.OrderChangeStatusException;
import com.kaycheung.order_service.exception.domain.order.OrderInvalidStatusValueException;
import com.kaycheung.order_service.exception.domain.order.OrderNotFoundException;
import com.kaycheung.order_service.messaging.outbox.OutboxEventService;
import com.kaycheung.order_service.messaging.outbox.OutboxEventType;
import com.kaycheung.order_service.messaging.outbox.payload.OrderCanceledPayload;
import com.kaycheung.order_service.repository.OrderItemRepository;
import com.kaycheung.order_service.repository.OrderRepository;
import com.kaycheung.order_service.util.ObjectMapperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalOrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapperUtils objectMapperUtils;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void updateOrderStatus(UUID orderId, InternalUpdateOrderStatusDTO request) {
        OrderStatus newOrderStatus;
        try {
            newOrderStatus = OrderStatus.valueOf(request.newOrderStatus());
        } catch (IllegalArgumentException ex) {
            throw new OrderInvalidStatusValueException(request.newOrderStatus());
        }

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setOrderStatus(newOrderStatus);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus orderStatus = order.getOrderStatus();
        if (orderStatus.equals(OrderStatus.CANCELED) || orderStatus.equals(OrderStatus.EXPIRED)) {
            return;
        }

        if (!order.getOrderStatus().equals(OrderStatus.PENDING_PAYMENT) && !order.getOrderStatus().equals(OrderStatus.PAYMENT_FAILED)) {
            throw new OrderChangeStatusException("Cannot cancel order in status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELED);
        orderRepository.save(order);

        OrderCanceledPayload payloadObject = new OrderCanceledPayload(order.getSourceQuoteId());
        String payload = objectMapperUtils.toJson(payloadObject);
        String key = "order:quote" + order.getSourceQuoteId() + "order_canceled";
        outboxEventService.createOutboxEvent(OutboxEventType.ORDER_CANCELED, payload, key);
    }
}
