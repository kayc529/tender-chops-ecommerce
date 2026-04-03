package com.kaycheung.payment_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.kaycheung.payment_service.messaging.inbox.InboxEvent;
import com.kaycheung.payment_service.messaging.inbox.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/inbox")
@RequiredArgsConstructor
public class InboxController {
    /**
     * V1 (demo): payment-service publishes outbox events by calling these endpoints over HTTP.
     * IMPORTANT:
     * - Keep these endpoints internal-only (service token / IP allowlist / gateway rules).
     * - Body is the outbox payload JSON string.
     * - Later you can swap this controller to insert inbox rows instead of handling inline.
     */
    private static final Logger log = LoggerFactory.getLogger(InboxController.class);
    private final InboxEventRepository inboxEventRepository;

    @PostMapping("/order-ready-to-capture")
    public ResponseEntity<Void> onOrderReadyToCapture(@RequestBody InboxMessage request) {
        return persistInboxEvent("order-ready-to-capture", request);
    }

    @PostMapping("/order-do-not-capture")
    public ResponseEntity<Void> onOrderDoNotCapture(@RequestBody InboxMessage request) {
        return persistInboxEvent("order-do-not-capture", request);
    }

    @PostMapping("/order-canceled")
    public ResponseEntity<Void> onOrderCanceled(@RequestBody InboxMessage request) {
        return persistInboxEvent("order-canceled", request);
    }


    private ResponseEntity<Void> persistInboxEvent(String endpoint, InboxMessage request) {
        String orderId = (request.payload() != null && request.payload().hasNonNull("orderId"))
                ? request.payload().get("orderId").asText()
                : "(none)";
        log.info("INBOX {} source={} messageId={} eventType={} orderId={}",
                endpoint, request.source(), request.messageId(), request.eventType(), orderId);

        InboxEvent inboxEvent = buildInboxEvent(request);
        if (inboxEvent == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            inboxEventRepository.save(inboxEvent);
        } catch (DataIntegrityViolationException ex) {
            log.warn("INBOX duplicate message ignored endpoint={} messageId={}", endpoint, request.messageId());
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            log.error("INBOX failed to persist message endpoint={} messageId={} eventType={}",
                    endpoint, request.messageId(), request.eventType(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.noContent().build();
    }

    private InboxEvent buildInboxEvent(InboxMessage inboxMessage) {
        if (inboxMessage.payload() == null || inboxMessage.payload().isNull()) {
            return null;
        }

        return InboxEvent.builder()
                .source(inboxMessage.source())
                .messageId(inboxMessage.messageId())
                .eventType(inboxMessage.eventType())
                .payload(inboxMessage.payload().toString())
                .receivedAt(Instant.now())
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();
    }

    /**
     * V1 (demo): envelope for outbox -> inbox publishing over HTTP.
     * This matches payment-service's OutboxPublishRequest.
     * <p>
     * payload is arbitrary JSON (already embedded as JSON by payment-service).
     */
    public record InboxMessage(
            String source,
            String messageId,
            String eventType,
            Instant occurredAt,
            JsonNode payload
    ) {
    }
}
