package com.kaycheung.inventory_service.config;

import com.kaycheung.inventory_service.config.properties.AwsMessagingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(AwsMessagingProperties.class)
public class AwsMessagingConfig {

    @Bean
    public SnsClient snsClient(AwsMessagingProperties properties) {
        return SnsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }

    @Bean
    public SqsClient sqsClient(AwsMessagingProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
