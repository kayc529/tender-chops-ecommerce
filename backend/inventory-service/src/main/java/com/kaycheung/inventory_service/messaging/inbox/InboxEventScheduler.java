package com.kaycheung.inventory_service.messaging.inbox;

import com.kaycheung.inventory_service.messaging.SqsIntakeWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventScheduler {
    private final InboxEventWorker inboxEventWorker;
    private final SqsIntakeWorker sqsIntakeWorker;

    @Scheduled(fixedDelayString = "${messaging.inbox.fixed-delay-ms:5000}")
    public void run() {
        try {
            inboxEventWorker.processInboxEvents();
            sqsIntakeWorker.pollAndPersist();
        } catch (Exception ex) {
            // Never crash the scheduler thread; log and continue.
            log.warn("InboxEventScheduler failed", ex);
        }
    }
}