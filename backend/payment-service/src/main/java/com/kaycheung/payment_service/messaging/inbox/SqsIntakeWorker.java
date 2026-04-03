package com.kaycheung.payment_service.messaging.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaycheung.payment_service.config.properties.AwsMessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsIntakeWorker {
    private final ObjectMapper objectMapper;
    private final InboxMessagePoller inboxMessagePoller;
    private final InboxEventService inboxEventService;
    private final SqsClient sqsClient;
    private final AwsMessagingProperties awsMessagingProperties;

    public void pollAndPersist() {
        //  poll from SQS queue
        ReceiveMessageResponse response = inboxMessagePoller.receiveOnce();

        for (Message message : response.messages()) {
            InboxEventService.PersistResult result;
            InboxEvent inboxEvent;

            try {
                inboxEvent = toInboxEvent(message);
                result = inboxEventService.persistInboxEvent(inboxEvent);
            } catch (Exception ex) {
                log.warn("Failed to persist SQS message into inbox. sqsMessageId={}. Message will not be deleted and will be retried by SQS.",
                        message.messageId(),
                        ex);
                continue;
            }

            if (result == InboxEventService.PersistResult.INSERTED || result == InboxEventService.PersistResult.DUPLICATE) {
                try {
                    deleteMessage(message);
                    log.info("SQS message persisted to inbox and deleted from queue. sqsMessageId={}, source={}, messageId={}, eventType={}, result={}",
                            message.messageId(),
                            inboxEvent.getSource(),
                            inboxEvent.getMessageId(),
                            inboxEvent.getEventType(),
                            result);
                } catch (Exception ex) {
                    log.warn("Failed to delete SQS message after inbox persistence. sqsMessageId={}, source={}, messageId={}, eventType={}, result={}. Message may be redelivered by SQS.",
                            message.messageId(),
                            inboxEvent.getSource(),
                            inboxEvent.getMessageId(),
                            inboxEvent.getEventType(),
                            result,
                            ex);
                }

            }
        }
    }

    private InboxEvent toInboxEvent(Message message) throws Exception {
        InboxMessage inboxMessage = objectMapper.readValue(message.body(), InboxMessage.class);

        if (inboxMessage.payload() == null || inboxMessage.payload().isNull()) {
            throw new IllegalArgumentException("Inbox message payload is null");
        }

        Instant now = Instant.now();

        return InboxEvent.builder()
                .source(inboxMessage.source())
                .messageId(inboxMessage.messageId())
                .eventType(inboxMessage.eventType())
                .payload(inboxMessage.payload().toString())
                .receivedAt(now)
                .attemptCount(0)
                .nextAttemptAt(now)
                .dead(false)
                .build();
    }

    private void deleteMessage(Message message) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(awsMessagingProperties.getSqs().getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
    }

    private record InboxMessage(
            String source,
            String messageId,
            String eventType,
            Instant occurredAt,
            JsonNode payload
    ) {
    }
}
