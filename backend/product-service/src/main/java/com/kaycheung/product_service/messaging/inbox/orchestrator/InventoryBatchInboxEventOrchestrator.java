package com.kaycheung.product_service.messaging.inbox.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.product_service.entity.StockAvailabilityStatus;
import com.kaycheung.product_service.messaging.inbox.InboxEvent;
import com.kaycheung.product_service.messaging.inbox.InboxEventProcessResult;
import com.kaycheung.product_service.service.ProductStockPersistService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryBatchInboxEventOrchestrator {

    private final ProductStockPersistService productStockPersistService;
    private final ObjectMapper objectMapper;


    public List<InboxEventProcessResult> onInventoryStockUpdated(List<InboxEvent> inboxEvents) {
        List<InboxEventProcessResult> results = new ArrayList<>();

        List<ParsedEvent> parsedEvents = new ArrayList<>();

        for (InboxEvent inboxEvent : inboxEvents) {
            try {
                ParsedEvent parsed = parse(inboxEvent);
                parsedEvents.add(parsed);
            } catch (Exception ex) {
                results.add(InboxEventProcessResult.failed(
                        inboxEvent,
                        "Failed to parse payload for inboxEventId=" + inboxEvent.getId() + ": " + ex.getMessage()
                ));
            }
        }

        //  group by productId
        Map<UUID, List<ParsedEvent>> grouped = parsedEvents.stream().collect(Collectors.groupingBy(ParsedEvent::productId));

        for (Map.Entry<UUID, List<ParsedEvent>> entry : grouped.entrySet()) {
            List<ParsedEvent> group = entry.getValue();

            //  sort by stockVersion DESC
            group.sort(Comparator.comparingLong(ParsedEvent::stockVersion).reversed());

            ParsedEvent winner = group.get(0);

            //  mark older events as SUCCESS
            for (int i = 1; i < group.size(); i++) {
                results.add(InboxEventProcessResult.success(group.get(i).event()));
            }

            try {
                int update = productStockPersistService.updateProductStock(winner.productId(), winner.availableStock(), winner.availabilityStatus(), winner.stockVersion());
                results.add(InboxEventProcessResult.success(winner.event()));
            } catch (Exception ex) {
                results.add(InboxEventProcessResult.failed(winner.event(), ex.getMessage()));
            }
        }

        return results;
    }

    private ParsedEvent parse(InboxEvent inboxEvent) {
        try {
            InventoryStockUpdatedPayload payload =
                    objectMapper.readValue(inboxEvent.getPayload(), InventoryStockUpdatedPayload.class);

            return new ParsedEvent(
                    inboxEvent,
                    payload.getProductId(),
                    payload.getAvailableStock(),
                    payload.getAvailabilityStatus(),
                    payload.getStockVersion()
            );

        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Getter
    private static class InventoryStockUpdatedPayload {
        private UUID productId;
        private int availableStock;
        private StockAvailabilityStatus availabilityStatus;
        private long stockVersion;
    }

    private record ParsedEvent(InboxEvent event, UUID productId, int availableStock,
                               StockAvailabilityStatus availabilityStatus, long stockVersion) {
    }
}
