package com.kaycheung.product_service.messaging.inbox.handler;

import com.kaycheung.product_service.messaging.inbox.InboxEvent;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessResult;
import org.springframework.stereotype.Component;

@Component
public class InventoryInboxEventHandler implements InboxEventHandler {
    @Override
    public InboxEventProcessResult handleEvent(InboxEvent inboxEvent) {
        return null;
    }
}
