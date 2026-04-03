package com.kaycheung.payment_service.messaging.outbox;

import com.kaycheung.payment_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OutboxEventService {
    private final OutboxEventRepository outboxEventRepository;
    private final MessagingProperties messagingProperties;

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
