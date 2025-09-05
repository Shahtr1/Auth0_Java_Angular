package com.orders_api.debug;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Profile("dev")
@RestController
@RequestMapping("/api/whoami")
public class WhoAmIController {

    @GetMapping
    public Map<String, Object> whoAmI(Authentication authentication) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("authenticated", authentication != null && authentication.isAuthenticated());

        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();

            out.put("subject", jwt.getSubject());
            out.put("issuer", Optional.ofNullable(jwt.getIssuer()).map(Object::toString).orElse(null));
            out.put("audience", jwt.getAudience());
            out.put("expiresAt", jwt.getExpiresAt());
            out.put("scope", jwt.getClaimAsString("scope"));
            out.put("permissions", jwt.getClaimAsStringList("permissions"));
            out.put("authorities", token.getAuthorities().stream().map(Object::toString).toList());

            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("azp", jwt.getClaimAsString("azp"));
            raw.put("gty", jwt.getClaimAsString("gty"));
            out.put("rawClaimsSample", raw);
        }
        return out;
    }

}
