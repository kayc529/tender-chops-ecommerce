package com.kaycheung.payment_service.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsMessagingProperties {
    @NotBlank
    private String region;
    @Valid
    private final Sns sns = new Sns();
    @Valid
    private final Sqs sqs = new Sqs();

    @Getter
    @Setter
    public static class Sns {
        @NotBlank
        private String topicArn;
    }

    @Getter
    @Setter
    public static class Sqs {
        @NotBlank
        private String queueUrl;
        private final Poll poll = new Poll();

        @Getter
        @Setter
        public static class Poll {
            @Min(1)
            @Max(10)
            private int maxMessages = 10;

            @Min(1)
            @Max(20)
            private int waitTimeSeconds = 10;
        }
    }
}