package com.kaycheung.cart_service.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> roles) {
                for (Object roleObj : roles) {
                    if (roleObj instanceof String roleName) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
                    }
                }
            }
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            for (Object clientAccessObj : resourceAccess.values()) {
                if (clientAccessObj instanceof Map<?, ?> clientAccess) {
                    Object rolesObj = clientAccess.get("roles");
                    if (rolesObj instanceof List<?> roles) {
                        for (Object roleObj : roles) {
                            if (roleObj instanceof String roleName) {
                                //  client roles are treated as authorities (not ROLE_ prefix)
                                authorities.add(new SimpleGrantedAuthority(roleName));
                            }
                        }
                    }
                }
            }
        }

        return authorities;
    }
}