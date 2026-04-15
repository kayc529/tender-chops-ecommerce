package com.kaycheung.inventory_service.messaging.inbox.handler;

import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import com.kaycheung.inventory_service.messaging.inbox.InboxEventType;
import com.kaycheung.inventory_service.messaging.inbox.orchestrator.ProductInboxEventOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInboxEventHandler implements InboxEventHandler {

    private final ProductInboxEventOrchestrator productInboxEventOrchestrator;

    @Override
    public void handleEvent(InboxEvent inboxEvent) {
        InboxEventType eventType = InboxEventType.from(inboxEvent.getEventType());

        switch (eventType) {
            case PRODUCT_CREATED -> onProductCreated(inboxEvent);
            default -> throw new IllegalArgumentException("Unsupported payment InboxEventType: " + eventType);
        }
    }

    private void onProductCreated(InboxEvent inboxEvent) {
        productInboxEventOrchestrator.handleProductCreated(inboxEvent);
    }
}
