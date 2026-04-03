package com.kaycheung.payment_service.config;

import com.kaycheung.payment_service.config.properties.StripeProperties;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StripeConfig {
    private final StripeProperties stripeProperties;

    @PostConstruct
    public void setup() {
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

}
