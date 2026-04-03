package com.kaycheung.payment_service.messaging.outbox;

import com.kaycheung.payment_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventWorker {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventWorker.class);
    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final MessagingProperties messagingProperties;
    private final OutboxEventService outboxEventService;


    public void publishOutboxEvents() {
        Instant now = Instant.now();
        int batchSize = messagingProperties.getOutbox().getBatchSize();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findUnpublishedOutboxEvents(now, PageRequest.of(0, batchSize));

        for (OutboxEvent outboxEvent : outboxEvents) {
            try {
                outboxEventPublisher.publish(outboxEvent);

                try {
                    outboxEventService.updateOutboxEventOnSuccess(outboxEvent, now);
                } catch (Exception ignored) {
                    //  best effort
                }
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event eventId={}, eventType={}", outboxEvent.getId(), outboxEvent.getEventType(), ex);
                try {
                    String lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    outboxEventService.updateOutboxEventOnFailure(outboxEvent, now, lastError);
                } catch (Exception ignored) {
                    //  best effort
                }
            }
        }
    }
}
