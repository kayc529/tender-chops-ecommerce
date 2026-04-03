package com.kaycheung.payment_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "messaging")
@Getter
@Setter
public class MessagingProperties {
    private Outbox outbox = new Outbox();
    private Inbox inbox = new Inbox();

    @Getter
    @Setter
    public static class Outbox {
        private int batchSize;
        private long fixedDelayMs;
        private int nextAttemptBackoffMin;
    }

    @Getter
    @Setter
    public static class Inbox {
        private int batchSize;
        private long fixedDelayMs;
        private int nextAttemptBackoffMin;
        private int maxRetries;
    }
}
