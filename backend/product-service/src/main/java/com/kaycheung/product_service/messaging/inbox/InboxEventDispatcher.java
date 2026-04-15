package com.kaycheung.product_service.messaging.inbox;

import com.kaycheung.product_service.messaging.inbox.handler.InventoryBatchInboxEventHandler;
import com.kaycheung.product_service.messaging.inbox.handler.InventoryInboxEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxEventDispatcher {

    private final InventoryInboxEventHandler inventoryInboxEventHandler;
    private final InventoryBatchInboxEventHandler inventoryBatchInboxEventHandler;

    public List<InboxEventProcessResult> dispatchEvent(List<InboxEvent> inboxEvents) {
        List<InboxEventProcessResult> results = new ArrayList<>();

        Map<InboxEventType, List<InboxEvent>> groupedEvents = inboxEvents.stream()
                .collect(Collectors.groupingBy(event ->
                        InboxEventType.from(event.getEventType())
                ));

        for (Map.Entry<InboxEventType, List<InboxEvent>> entry : groupedEvents.entrySet()) {
            InboxEventType eventType = entry.getKey();
            List<InboxEvent> events = entry.getValue();

            log.debug("Dispatching {} events for eventType={}", events.size(), eventType);

            switch (eventType) {
                case INVENTORY_STOCK_UPDATED -> {
                    List<InboxEventProcessResult> processResults = inventoryBatchInboxEventHandler.handleEvents(events);
                    results.addAll(processResults);
                }
                default -> {
                    for (InboxEvent event : events) {
                        results.add(InboxEventProcessResult.dead(event, "Unknown event type"));
                    }
                }
            }
        }

        return results;
    }

}
