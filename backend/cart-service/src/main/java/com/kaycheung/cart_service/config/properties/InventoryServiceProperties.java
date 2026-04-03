package com.kaycheung.cart_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "inventory-service")
@Getter
@Setter
public class InventoryServiceProperties {
    private String baseUrl;
    private Timeout timeout;

    @Getter
    @Setter
    public static class Timeout {
        private int connectMs;
        private int responseMs;
    }
}
