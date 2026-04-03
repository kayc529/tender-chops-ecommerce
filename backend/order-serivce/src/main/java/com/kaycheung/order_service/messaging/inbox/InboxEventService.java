package com.kaycheung.order_service.messaging.inbox;

import com.kaycheung.order_service.config.properties.MessagingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class InboxEventService {

    private final InboxEventRepository inboxEventRepository;
    private final MessagingProperties messagingProperties;

    @Transactional
    public int updateInboxEventOnSuccess(InboxEvent inboxEvent, Instant now) {
        return inboxEventRepository.updateInboxEventOnSuccess(inboxEvent.getId(), now);
    }

    @Transactional
    public int updateInboxEventOnFailure(InboxEvent inboxEvent, Instant now, String lastError)
    {
        int backoffMin = messagingProperties.getInbox().getNextAttemptBackoffMin();
        Instant nextAttemptAt = now.plus(backoffMin, ChronoUnit.MINUTES);
        return inboxEventRepository.updateInboxEventOnFailure(inboxEvent.getId(), nextAttemptAt, lastError);
    }

    @Transactional
    public int markInboxEventDead(InboxEvent inboxEvent)
    {
        return inboxEventRepository.markInboxEventDead(inboxEvent.getId());
    }

    @Transactional
    public PersistResult persistInboxEvent(InboxEvent inboxEvent) {
        try {
            inboxEventRepository.save(inboxEvent);
            return PersistResult.INSERTED;
        } catch (DataIntegrityViolationException ex) {
            return PersistResult.DUPLICATE;
        }
    }

    public enum PersistResult {
        INSERTED,
        DUPLICATE
    }
}
