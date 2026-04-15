package com.kaycheung.product_service.messaging.inbox;

import com.kaycheung.product_service.config.properties.MessagingProperties;
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

        if(inboxEvents.isEmpty())
        {
            return;
        }

        List<InboxEventProcessResult> processResults = inboxEventDispatcher.dispatchEvent(inboxEvents);

        for (InboxEventProcessResult processResult : processResults) {
            InboxEventProcessStatus processStatus = processResult.status();
            InboxEvent inboxEvent = processResult.inboxEvent();

            switch (processStatus) {
                case SUCCESS -> {
                    int updated = inboxEventService.updateInboxEventOnSuccess(inboxEvent, now);
                    if (updated == 0) {
                        log.debug("Inbox event already processed, skipping: id={} source={} messageId={} type={}",
                                inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                    }
                }
                case FAILED -> {
                    int maxRetries = messagingProperties.getInbox().getMaxRetries();

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
                    int updated = inboxEventService.updateInboxEventOnFailure(inboxEvent, nextAttemptAt, processResult.errorString());

                    if (updated == 0) {
                        log.debug("Inbox event already processed, skipping failure update: id={} source={} messageId={} type={}",
                                inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                        continue;
                    }

                    log.warn("Failed to process inbox event. Will retry: id={} source={} messageId={} type={} attempt={} nextAttemptAt={}",
                            inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType(), attempt, nextAttemptAt);
                }
                case DEAD -> {
                    int updated = inboxEventService.markInboxEventDead(inboxEvent);

                    if (updated == 0) {
                        log.debug("Inbox event already processed, skipping mark as dead for poison message: id={} source={} messageId={} type={}",
                                inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                        continue;
                    }

                    log.error("Poison inbox event (cannot dispatch). Marked as dead: id={} source={} messageId={} type={}",
                            inboxEvent.getId(), inboxEvent.getSource(), inboxEvent.getMessageId(), inboxEvent.getEventType());
                }
            }

        }
    }
}
