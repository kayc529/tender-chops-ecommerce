package com.kaycheung.order_service.messaging.outbox;

import com.kaycheung.order_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxEventWorker {

    private final MessagingProperties messagingProperties;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxEventService outboxEventService;

    public void publishOutboxEvents() {
        Instant now = Instant.now();
        int batchSize = messagingProperties.getOutbox().getBatchSize();
        List<OutboxEvent> outboxEvents = outboxEventRepository.findUnpublishedOutboxEvents(now, PageRequest.of(0, batchSize));

        if (outboxEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent outboxEvent : outboxEvents) {
            try {
                outboxEventPublisher.publish(outboxEvent);
                try {
                    outboxEventService.updateOutboxEventOnSuccess(outboxEvent, now);
                } catch (Exception ignored) {
                    //  best-effort
                }
            } catch (Exception ex) {
                String lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                try {
                    outboxEventService.updateOutboxEventOnFailure(outboxEvent, now, lastError);
                } catch (Exception ignored) {
                    //  best-effort
                }
            }
        }
    }
}
