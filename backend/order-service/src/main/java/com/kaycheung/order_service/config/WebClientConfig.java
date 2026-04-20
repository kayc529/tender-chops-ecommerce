package com.kaycheung.order_service.config;

import com.kaycheung.order_service.config.properties.CartServiceProperties;
import com.kaycheung.order_service.config.properties.InventoryServiceProperties;
import com.kaycheung.order_service.config.properties.PaymentServiceProperties;
import com.kaycheung.order_service.config.properties.ProductServiceProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    @Primary
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
    @Qualifier("internalAuthorizedClientManager")
    public OAuth2AuthorizedClientManager internalAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    @Qualifier("productWebClient")
    public WebClient productWebClient(OAuth2AuthorizedClientManager manager, ProductServiceProperties productServiceProperties) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("order-service");

        int connectionTimeout = productServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = productServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(productServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }

    @Bean
    @Qualifier("inventoryWebClient")
    public WebClient inventoryWebClient(OAuth2AuthorizedClientManager manager, InventoryServiceProperties inventoryServiceProperties) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("order-service");

        int connectionTimeout = inventoryServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = inventoryServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(inventoryServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }

    @Bean
    @Qualifier("inventoryInternalWebClient")
    public WebClient inventoryInternalWebClient(
            @Qualifier("internalAuthorizedClientManager") OAuth2AuthorizedClientManager manager,
            InventoryServiceProperties inventoryServiceProperties
    ) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("order-service");

        int connectionTimeout = inventoryServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = inventoryServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(inventoryServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }


    @Bean
    @Qualifier("cartWebClient")
    public WebClient cartWebClient(OAuth2AuthorizedClientManager manager, CartServiceProperties cartServiceProperties) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("order-service");

        int connectionTimeout = cartServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = cartServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(cartServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }

    @Bean
    @Qualifier("paymentWebClient")
    public WebClient paymentWebClient(OAuth2AuthorizedClientManager manager, PaymentServiceProperties paymentServiceProperties) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("order-service");

        int connectionTimeout = paymentServiceProperties.getTimeout().getConnectMs();
        int responseTimeout = paymentServiceProperties.getTimeout().getResponseMs();

        HttpClient httpClient = createHttpClient(connectionTimeout, responseTimeout);

        return WebClient.builder()
                .baseUrl(paymentServiceProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2.oauth2Configuration())
                .build();
    }

    private HttpClient createHttpClient(int connectionTimeout, int responseTimeout) {
        return HttpClient.create()
                // TCP connect timeout
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                // Max time from request sent to full response received
                .responseTimeout(Duration.ofMillis(responseTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeout, TimeUnit.MILLISECONDS))
                );
    }
}
