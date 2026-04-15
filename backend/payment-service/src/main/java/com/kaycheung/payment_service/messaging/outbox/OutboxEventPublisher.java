package com.kaycheung.payment_service.messaging.outbox;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.payment_service.config.properties.AwsMessagingProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);
    private final ObjectMapper objectMapper;
    private final SnsClient snsClient;
    private final AwsMessagingProperties awsMessagingProperties;


    public void publish(OutboxEvent outboxEvent) {
        OutboxEventType type = resolveType(outboxEvent);

        String payloadJson = outboxEvent.getPayload();

        OutboxPublishRequest request = new OutboxPublishRequest(
                "payment-service",
                outboxEvent.getIdempotencyKey(),
                type.name(),
                outboxEvent.getOccurredAt() != null ? outboxEvent.getOccurredAt() : Instant.now(),
                payloadJson
        );

        try {
            String messageJson = objectMapper.writeValueAsString(request);

            Map<String, MessageAttributeValue> messageAttributes = Map.of(
                    "eventType", MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(type.name())
                            .build()
            );

            PublishRequest publishRequest = PublishRequest.builder()
                    .topicArn(awsMessagingProperties.getSns().getTopicArn())
                    .message(messageJson)
                    .messageAttributes(messageAttributes)
                    .build();

            snsClient.publish(publishRequest);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event. eventType={}, idempotencyKey={}",
                    outboxEvent.getEventType(),
                    outboxEvent.getIdempotencyKey(),
                    ex);
            throw new RuntimeException(
                    "Failed to publish outbox event. eventType=" + outboxEvent.getEventType()
                            + ", idempotencyKey=" + outboxEvent.getIdempotencyKey(),
                    ex
            );
        }
    }

    private OutboxEventType resolveType(OutboxEvent outboxEvent) {
        // Supports either enum-typed field or String-typed field (depending on your entity).
        try {
            String raw = outboxEvent.getEventType();
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("eventType is null/blank");
            }
            return OutboxEventType.valueOf(raw);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown outbox event type: " + outboxEvent.getEventType());
        }
    }

    public record OutboxPublishRequest(
            String source,
            String messageId,
            String eventType,
            Instant occurredAt,
            @JsonRawValue String payload
    ) {
    }
}
