package com.kaycheung.product_service.messaging.inbox.handler;

import com.kaycheung.product_service.messaging.inbox.InboxEvent;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessResult;
import com.kaycheung.product_service.messaging.inbox.InboxEventType;

import java.util.List;

public interface BatchInboxEventHandler {
    List<InboxEventProcessResult> handleEvents(List<InboxEvent> inboxEvents);
}
