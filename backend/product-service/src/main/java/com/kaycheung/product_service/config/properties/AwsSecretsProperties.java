package com.kaycheung.product_service.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "aws.secrets")
@Getter
@Setter
public class AwsSecretsProperties {
    private String imageProcessorCallbackSecret;
}
