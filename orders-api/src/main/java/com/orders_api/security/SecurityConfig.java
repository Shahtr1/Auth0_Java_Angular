package com.orders_api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.*;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            Converter<Jwt, ? extends AbstractAuthenticationToken> authConverter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAuthority("SCOPE_read:orders")
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAuthority("SCOPE_write:orders")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(authConverter)));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            OAuth2TokenValidator<Jwt> audienceValidator) {

        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    @Bean
    OAuth2TokenValidator<Jwt> audienceValidator(@Value("${auth0.audience}") String audience) {
        return jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && aud.contains(audience))
                return OAuth2TokenValidatorResult.success();
            OAuth2Error err = new OAuth2Error("invalid_token",
                    "Required audience " + audience + " is missing", null);
            return OAuth2TokenValidatorResult.failure(err);
        };
    }

    /**
     * Convert JWT claims -> Spring authorities.
     * - Map Auth0 RBAC `permissions` (array) to SCOPE_permissions
     * - Also map standard `scope` (space-delimited) to SCOPE_x
     */
    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
        return jwt -> {
            Set<GrantedAuthority> authorities = new HashSet<>();

            // 1) Auth0 RBAC permissions claim (if present)
            List<String> perms = jwt.getClaimAsStringList("permissions");
            if (perms != null) {
                for (String p : perms) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + p));
                }
            }

            // 2) Standard OAuth2 scopes (space-delimited)
            String scope = jwt.getClaimAsString("scope");
            if (scope != null) {
                for (String s : scope.split(" ")) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                }
            }

            return new JwtAuthenticationToken(jwt, authorities);
        };
    }

    // CORS for local dev (Angular at :4200)
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        cfg.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
