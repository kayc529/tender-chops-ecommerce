package com.kaycheung.product_service.messaging.outbox;

import com.kaycheung.product_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final MessagingProperties messagingProperties;

    public void createOutboxEvent(OutboxEventType eventType, String payload, String idempotencyKey) {
        Objects.requireNonNull(eventType, "eventType");

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        Instant now = Instant.now();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .idempotencyKey(idempotencyKey)
                .eventType(eventType.name())
                .payload(payload)
                .occurredAt(now)
                .publishedAt(null)
                .attemptCount(0)
                .lastError(null)
                .nextAttemptAt(now)
                .build();

        try {
            outboxEventRepository.save(outboxEvent);
        } catch (DataIntegrityViolationException ex) {
            // Duplicate delivery or duplicate handler run — safe to ignore.
            log.info("OutboxEvent already exists (idempotent): key={}, type={}", idempotencyKey, eventType);
        }
    }

    @Transactional
    public void updateOutboxEventOnSuccess(OutboxEvent outboxEvent, Instant now) {
        outboxEventRepository.updateOutboxEventOnSuccess(outboxEvent.getId(), now);
    }

    @Transactional
    public void updateOutboxEventOnFailure(OutboxEvent outboxEvent, Instant now, String lastError)
    {
        int backoffMin = messagingProperties.getOutbox().getNextAttemptBackoffMin();
        Instant nextAttemptAt = now.plus(backoffMin, ChronoUnit.MINUTES);
        outboxEventRepository.updateOutboxEventOnFailure(outboxEvent.getId(), nextAttemptAt, lastError);
    }

}