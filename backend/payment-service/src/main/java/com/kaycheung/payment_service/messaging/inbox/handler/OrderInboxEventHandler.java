package com.kaycheung.payment_service.messaging.inbox.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.payment_service.messaging.inbox.InboxEvent;
import com.kaycheung.payment_service.messaging.inbox.InboxEventType;
import com.kaycheung.payment_service.messaging.inbox.orchestrator.OrderInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInboxEventHandler implements InboxEventHandler {
    private final ObjectMapper objectMapper;
    private final OrderInboxEventOrchestrator orderInboxEventOrchestrator;

    @Override
    public void handleEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.valueOf(inboxEvent.getEventType());

        switch (eventType) {
            case ORDER_READY_TO_CAPTURE -> onOrderReadyToCapture(inboxEvent);
            case ORDER_DO_NOT_CAPTURE -> onOrderDoNotCapture(inboxEvent);
            case ORDER_CANCELED -> onOrderCanceled(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported InboxEventType: " + eventType);
        }
    }

    private void onOrderReadyToCapture(InboxEvent inboxEvent) {
        OrderEventPayload p = requireOrderPayload(inboxEvent);
        log.info("Handling ORDER_READY_TO_CAPTURE inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());

        orderInboxEventOrchestrator.handleOrderReadyToCapture(p);
    }

    private void onOrderDoNotCapture(InboxEvent inboxEvent) {
        OrderEventPayload p = requireOrderPayload(inboxEvent);
        log.info("Handling ORDER_DO_NOT_CAPTURE inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={} reason={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId(), p.reason());

        orderInboxEventOrchestrator.handleOrderDoNotCapture(p);
    }

    private void onOrderCanceled(InboxEvent inboxEvent) {
        OrderEventPayload p = requireOrderPayload(inboxEvent);
        log.info("Handling ORDER_CANCELED inboxEventId={} key={} orderId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId());

        orderInboxEventOrchestrator.handleOrderCanceled(p);
    }

    private OrderEventPayload requireOrderPayload(InboxEvent inboxEvent) {
        String raw = inboxEvent.getPayload();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing payload for order eventType=" + inboxEvent.getEventType() + ", inboxEventId=" + inboxEvent.getId());
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            UUID orderId = readUuid(root, "orderId");
            UUID paymentId = readUuidNullable(root, "paymentId");
            UUID paymentAttemptId = readUuidNullable(root, "paymentAttemptId");
            JsonNode reasonNode = root.get("reason");
            String reason = (reasonNode == null || reasonNode.isNull()) ? null : reasonNode.asText(null);
            return new OrderEventPayload(orderId, paymentId, paymentAttemptId, reason);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON payload for inboxEventId=" + inboxEvent.getId() + ", eventType=" + inboxEvent.getEventType(), e);
        }
    }

    public record OrderEventPayload(UUID orderId, UUID paymentId, UUID paymentAttemptId, String reason) {
    }

    private UUID readUuid(JsonNode root, String field) {
        UUID v = readUuidNullable(root, field);
        if (v == null) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in order event payload");
        }
        return v;
    }

    private UUID readUuidNullable(JsonNode root, String field) {
        if (root == null) return null;
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) return null;
        String s = n.asText(null);
        if (s == null || s.isBlank()) return null;
        return UUID.fromString(s);
    }
}
