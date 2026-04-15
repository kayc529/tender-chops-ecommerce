package com.kaycheung.inventory_service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.inventory_service.messaging.inbox.InboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InboxEventPayloadUtil {
    private final ObjectMapper objectMapper;

    public <T> T parsePayload(InboxEvent inboxEvent, Class<T> payloadType) {
        try {
            String rawPayload = inboxEvent.getPayload();
            if (rawPayload == null || rawPayload.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing payload for eventType=" + inboxEvent.getEventType() +
                                ", inboxEventId=" + inboxEvent.getId()
                );
            }
            return objectMapper.readValue(rawPayload, payloadType);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Malformed JSON payload for inboxEventId=" + inboxEvent.getId() +
                            ", eventType=" + inboxEvent.getEventType() + ", payloadType=" + payloadType.getSimpleName(),
                    ex
            );
        }
    }
}
