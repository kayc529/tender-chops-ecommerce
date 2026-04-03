package com.kaycheung.order_service.messaging.inbox.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.order_service.messaging.inbox.InboxEvent;
import com.kaycheung.order_service.messaging.inbox.InboxEventType;
import com.kaycheung.order_service.messaging.inbox.orchestrator.PaymentInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInboxEventHandler implements InboxEventHandler {

    private final ObjectMapper objectMapper;
    private final PaymentInboxEventOrchestrator paymentInboxEventOrchestrator;

    @Override
    public void handleEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.valueOf(inboxEvent.getEventType());
        switch (eventType) {
            case PAYMENT_ATTEMPT_AUTHORIZED -> onPaymentAttemptAuthorized(inboxEvent);
            case PAYMENT_ATTEMPT_CANCELED -> onPaymentAttemptCanceled(inboxEvent);
            case PAYMENT_ATTEMPT_FAILED -> onPaymentAttemptFailed(inboxEvent);
            case PAYMENT_CAPTURED -> onPaymentCaptured(inboxEvent);
            case PAYMENT_CANCELED -> onPaymentCanceled(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported payment InboxEventType: " + eventType);
        }
    }

    private void onPaymentAttemptAuthorized(InboxEvent inboxEvent) {
        PaymentEventPayload p = requirePaymentPayload(inboxEvent);

        // Expected order status on entry: PENDING_PAYMENT/ PAYMENT_FAILED (retry)
        log.info("Handling PAYMENT_ATTEMPT_AUTHORIZED inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());

        paymentInboxEventOrchestrator.handlePaymentAttemptAuthorized(inboxEvent.getId(), p);

    }

    private void onPaymentAttemptCanceled(InboxEvent inboxEvent) {
        PaymentEventPayload p = requirePaymentPayload(inboxEvent);

        // Intentionally does NOT change order status in v1.
        // Rationale: a canceled *attempt* just means the customer backed out; order stays payable.

        log.info("Handling PAYMENT_ATTEMPT_CANCELED inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());
    }

    private void onPaymentAttemptFailed(InboxEvent inboxEvent) {
        PaymentEventPayload p = requirePaymentPayload(inboxEvent);

        log.info("Handling PAYMENT_ATTEMPT_FAILED inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());

        paymentInboxEventOrchestrator.handlePaymentAttemptFailed(inboxEvent.getId(), p);
    }

    private void onPaymentCaptured(InboxEvent inboxEvent) {
        PaymentEventPayload p = requirePaymentPayload(inboxEvent);

        log.info("Handling PAYMENT_CAPTURED inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());

        paymentInboxEventOrchestrator.handlePaymentCaptured(inboxEvent.getId(), p);
    }

    private void onPaymentCanceled(InboxEvent inboxEvent) {
        PaymentEventPayload p = requirePaymentPayload(inboxEvent);

        // Intentionally does NOT change order status in v1.
        // Rationale: a payment becomes terminal-canceled only because the order was canceled (order-service is source of truth).

        log.info("Handling PAYMENT_CANCELED inboxEventId={} key={} orderId={} paymentId={} paymentAttemptId={}",
                inboxEvent.getId(), inboxEvent.getMessageId(), p.orderId(), p.paymentId(), p.paymentAttemptId());
    }

    public record PaymentEventPayload(UUID orderId, UUID paymentId, UUID paymentAttemptId) {
    }

    private PaymentEventPayload requirePaymentPayload(InboxEvent inboxEvent) {
        String raw = inboxEvent.getPayload();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing payload for payment eventType=" + inboxEvent.getEventType() + ", inboxEventId=" + inboxEvent.getId());
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            UUID orderId = readUuid(root, "orderId");
            UUID paymentId = readUuidNullable(root, "paymentId");
            UUID paymentAttemptId = readUuidNullable(root, "paymentAttemptId");
            return new PaymentEventPayload(orderId, paymentId, paymentAttemptId);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed JSON payload for inboxEventId=" + inboxEvent.getId() + ", eventType=" + inboxEvent.getEventType(), e);
        }
    }

    private UUID readUuid(JsonNode root, String field) {
        UUID v = readUuidNullable(root, field);
        if (v == null) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in payment event payload");
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
