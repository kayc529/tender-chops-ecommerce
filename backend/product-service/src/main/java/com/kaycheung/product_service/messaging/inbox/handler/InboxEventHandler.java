package com.kaycheung.product_service.messaging.inbox.handler;

import com.kaycheung.product_service.messaging.inbox.InboxEvent;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessResult;

public interface InboxEventHandler {
    InboxEventProcessResult handleEvent(InboxEvent inboxEvent);
}
