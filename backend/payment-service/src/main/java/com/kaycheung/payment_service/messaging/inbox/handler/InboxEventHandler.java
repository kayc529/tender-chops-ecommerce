package com.kaycheung.payment_service.messaging.inbox.handler;

import com.kaycheung.payment_service.messaging.inbox.InboxEvent;

public interface InboxEventHandler {
    void handleEvent(InboxEvent inboxEvent);
}
