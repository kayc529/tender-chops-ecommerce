package com.kaycheung.payment_service.messaging.inbox;

import com.kaycheung.payment_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventWorker {

    private final InboxEventRepository inboxEventRepository;
    private final MessagingProperties messagingProperties;
    private final InboxEventDispatcher inboxEventDispatcher;
    private final InboxEventService inboxEventService;

    public void processInboxEvents() {
        Instant now = Instant.now();
        int configuredBatchSize = messagingProperties.getInbox().getBatchSize();
        int batchSize = configuredBatchSize > 0 ? configuredBatchSize : 10;

        List<InboxEvent> inboxEvents = inboxEventRepository.findDueUnprocessed(now, PageRequest.of(0, batchSize));

        for (InboxEvent inboxEvent : inboxEvents) {
            try {
                inboxEventDispatcher.dispatchEvent(inboxEvent);
                // Mark processed (idempotent: repository update only applies when processedAt is null)
                int updated = inboxEventService.updateInboxEventOnSuccess(inboxEvent, now);
                if (updated == 0) {
                    log.debug("Inbox event already processed, skipping: id={} source={} messageId={} type={}",
                            inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                }
            } catch (IllegalArgumentException ex) {
                // Invalid event type / cannot dispatch. This is effectively poison: retrying won't help.
                // TODO(v1.5): When we switch to SNS/SQS, move poison inbox events to a proper dead-letter queue/table
                String err = summarizeError(ex);
                int updated = inboxEventService.markInboxEventDead(inboxEvent);

                if (updated == 0) {
                    log.debug("Inbox event already processed, skipping mark as dead for poison message: id={} source={} messageId={} type={}",
                            inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                    continue;
                }

                log.error("Poison inbox event (cannot dispatch). Marked as dead: id={} source={} messageId={} type={} error={}",
                        inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType(), err);
            } catch (Exception ex) {
                int maxRetries = messagingProperties.getInbox().getMaxRetries();
                String err = summarizeError(ex);

                if (inboxEvent.getAttemptCount() >= maxRetries) {
                    int updated = inboxEventService.markInboxEventDead(inboxEvent);
                    if (updated == 0) {
                        log.debug("Inbox event already processed, skipping mark as dead for failed event processing: id={} source={} messageId={} type={}",
                                inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                    } else {
                        log.error("Inbox event exceeded max retries({}). Marked dead: id={} source={} messageId={} type={}", maxRetries, inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                    }
                    continue;
                }

                int attempt = Math.max(1, inboxEvent.getAttemptCount() + 1);
                int nextAttemptBackoffMin = messagingProperties.getInbox().getNextAttemptBackoffMin();
                Instant nextAttemptAt = now.plus(nextAttemptBackoffMin, ChronoUnit.MINUTES);
                int updated = inboxEventService.updateInboxEventOnFailure(inboxEvent, nextAttemptAt, err);

                if (updated == 0) {
                    log.debug("Inbox event already processed, skipping failure update: id={} source={} messageId={} type={}",
                            inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                    continue;
                }

                log.warn("Failed to process inbox event. Will retry: id={} source={} messageId={} type={} attempt={} nextAttemptAt={} error={}",
                        inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType(), attempt, nextAttemptAt, err);
            }
        }
    }

    private static String summarizeError(Throwable t) {
        if (t == null) {
            return "unknown";
        }
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName() + ": " + msg;
        }
        // Keep last_error bounded
        int max = 2000;
        if (msg.length() > max) {
            msg = msg.substring(0, max);
        }
        return msg;
    }
}
