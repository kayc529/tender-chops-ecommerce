package com.kaycheung.payment_service.messaging.inbox;

import com.kaycheung.payment_service.messaging.inbox.handler.OrderInboxEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboxEventDispatcher {
    private final OrderInboxEventHandler orderInboxEventHandler;

    public void dispatchEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.valueOf(inboxEvent.getEventType());

        switch (eventType) {
            case ORDER_READY_TO_CAPTURE,
                 ORDER_DO_NOT_CAPTURE,
                 ORDER_CANCELED -> orderInboxEventHandler.handleEvent(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported InboxEventType: " + eventType);
        }
    }
}
