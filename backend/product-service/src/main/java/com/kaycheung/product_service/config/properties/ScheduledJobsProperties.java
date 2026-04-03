package com.kaycheung.product_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobs")
@Getter
@Setter
public class ScheduledJobsProperties {
    private ProductStaleImageCleanup productStaleImageCleanup;

    @Getter
    @Setter
    public static class ProductStaleImageCleanup {
        private int batchSize = 10;
        private long fixedDelayMs;
    }
}
