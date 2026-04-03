package com.kaycheung.inventory_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //  Actuator
                        .requestMatchers("/actuator/health/**", "/actuator/info/**").permitAll()
                        //  Internal inventory management
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/inventory").hasAuthority("inventory:internal:create")
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/inventory/reservations").hasAuthority("inventory:internal:reserve")
                        .requestMatchers(HttpMethod.POST, "/api/v1/internal/inventory/reservations/release-*").hasAuthority("inventory:internal:release")

                        // GET single availability: admin OR service
                        .requestMatchers(HttpMethod.GET, "/api/v1/admin/inventory/*")
                        .hasAnyAuthority("ROLE_ADMIN", "inventory:internal:read")
                        // /batch: admin OR service
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/inventory/batch")
                        .hasAnyAuthority("ROLE_ADMIN", "inventory:internal:read")
                        // PUT / DELETE inventory: admin only
                        .requestMatchers(HttpMethod.PUT, "/api/v1/admin/inventory/*")
                        .hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/inventory/*")
                        .hasAuthority("ROLE_ADMIN")

                        //  Public inventory read
                        .requestMatchers("/api/v1/inventory/**").permitAll()

                        //  TODO temporary endpoints, delete when SNS/SQS implemented
                        .requestMatchers("/api/v1/internal/inbox/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
