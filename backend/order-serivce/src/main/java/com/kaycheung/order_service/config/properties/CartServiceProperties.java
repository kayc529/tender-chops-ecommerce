package com.kaycheung.order_service.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@Configuration
@ConfigurationProperties(prefix = "cart-service")
@Getter
@Setter
public class CartServiceProperties {
    private String baseUrl;
    private Timeout timeout;

    @Getter
    @Setter
    public static class Timeout {
        @Min(100)
        private int connectMs;
        @Min(100)
        private int responseMs;
    }
}
