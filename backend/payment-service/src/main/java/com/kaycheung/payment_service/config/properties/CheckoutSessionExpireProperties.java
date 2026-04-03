package com.kaycheung.payment_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "jobs.checkout-session-expire")
@Getter
@Setter
public class CheckoutSessionExpireProperties {
    private int batchSize;
    private long fixedDelayMs;
}
