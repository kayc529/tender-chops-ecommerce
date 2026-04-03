package com.kaycheung.inventory_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "inventory.reservation")
@Getter
@Setter
public class InventoryReservationProperties {
    private Duration ttl;
}
