package com.kaycheung.product_service.messaging.inbox;

import com.kaycheung.product_service.config.properties.AwsMessagingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

@Component
@RequiredArgsConstructor
public class InboxMessagePoller {
    private final SqsClient sqsClient;
    private final AwsMessagingProperties awsMessagingProperties;

    public ReceiveMessageResponse receiveOnce() {
        return sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(awsMessagingProperties.getSqs().getQueueUrl())
                        .maxNumberOfMessages(awsMessagingProperties.getSqs().getPoll().getMaxMessages())
                        .waitTimeSeconds(awsMessagingProperties.getSqs().getPoll().getWaitTimeSeconds())
                        .build()
        );
    }
}
