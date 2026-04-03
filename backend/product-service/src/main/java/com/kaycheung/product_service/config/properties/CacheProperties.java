package com.kaycheung.product_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class CacheProperties {
    private String keyVersion;
    private long productListTtlMinutes = 3;
    private long productDetailTtlMinutes = 15;
}
