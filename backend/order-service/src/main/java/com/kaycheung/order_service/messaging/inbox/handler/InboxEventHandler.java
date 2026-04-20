package com.kaycheung.order_service.messaging.inbox.handler;

import com.kaycheung.order_service.messaging.inbox.InboxEvent;

public interface InboxEventHandler {
    void handleEvent(InboxEvent inboxEvent);
}
