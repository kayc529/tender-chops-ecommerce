package com.kaycheung.order_service.messaging.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventWorker outboxEventWorker;

    @Scheduled(fixedDelayString = "${messaging.outbox.fixed-delay-ms:5000}")
    public void run() {
        try {
            outboxEventWorker.publishOutboxEvents();
        } catch (Exception ex) {
            // Never crash the scheduler thread; log and continue.
            log.warn("OutboxEventScheduler failed", ex);
        }
    }
}
