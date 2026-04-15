package com.kaycheung.inventory_service.messaging.inbox.handler;

import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;

public interface InboxEventHandler {
    void handleEvent(InboxEvent inboxEvent);
}
