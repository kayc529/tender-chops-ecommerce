package com.kaycheung.product_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        //  CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //  Actuator
                        .requestMatchers("/actuator/health/**", "/actuator/info/**").permitAll()
                        //  public API
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        //  admin only API
                        .requestMatchers("/api/v1/admin/products/**").hasRole("ADMIN")
                        //  Internal service-to-service API
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/products/**").hasAuthority("products:internal:read")
                        //  TODO(v2): delete this endpoint when lambda(image-processor) results -> SNS/SQS
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/product-images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
