package com.kaycheung.product_service.config;

import com.kaycheung.product_service.config.properties.InventoryServiceProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final InventoryServiceProperties properties;

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
        DefaultOAuth2AuthorizedClientManager manager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    @Qualifier("inventoryWebClient")
    public WebClient inventoryWebClient(OAuth2AuthorizedClientManager manager, InventoryServiceProperties inventoryServiceProperties) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("product-service");

        int connectionTimeout = inventoryServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = inventoryServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }

    private HttpClient createHttpClient(int connectionTimeout, int responseTimeout) {
        return HttpClient.create()
                // TCP connect timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                // Max time from request sent to full response received
                .responseTimeout(Duration.ofSeconds(responseTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS))
                );
    }
}
