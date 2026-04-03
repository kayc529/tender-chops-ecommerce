package com.kaycheung.order_service.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class SecurityUtils {

    public static UUID getCurrentUserIdUUID() {
        Jwt jwt = (Jwt) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
