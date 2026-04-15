package com.kaycheung.inventory_service.messaging.inbox.handler;

import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import com.kaycheung.inventory_service.messaging.inbox.InboxEventType;
import com.kaycheung.inventory_service.messaging.inbox.orchestrator.OrderInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderInboxEventHandler implements InboxEventHandler {

    private final OrderInboxEventOrchestrator orderInboxEventOrchestrator;

    @Override
    public void handleEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.from(inboxEvent.getEventType());

        switch (eventType) {
            case ORDER_CREATED -> onOrderCreated(inboxEvent);
            case ORDER_CANCELED -> onOrderCanceled(inboxEvent);
            case ORDER_CREATION_FAILED -> onOrderCreationFailed(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported payment InboxEventType: " + eventType);
        }
    }

    private void onOrderCreated(InboxEvent inboxEvent)
    {
        orderInboxEventOrchestrator.handleOrderCreated(inboxEvent);
    }

    private void onOrderCanceled(InboxEvent inboxEvent) {
        orderInboxEventOrchestrator.handleOrderCanceled(inboxEvent);
    }

    private void onOrderCreationFailed(InboxEvent inboxEvent){
        orderInboxEventOrchestrator.handleOrderCreationFailed(inboxEvent);
    }
}
