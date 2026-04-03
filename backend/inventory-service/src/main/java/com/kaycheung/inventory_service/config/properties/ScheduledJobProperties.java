package com.kaycheung.inventory_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobs")
@Getter
@Setter
public class ScheduledJobProperties {
    private ReservationExpiry reservationExpiry;

    @Getter
    @Setter
    public static class ReservationExpiry {
        private int batchSize;
        private long fixedDelayMs;
    }
}
