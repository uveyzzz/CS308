package com.cs308.store.user;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells the frontend who is logged in, so it can show the right pages.
 * This is only for display: SecurityConfig still checks the role on every request.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public Map<String, String> currentUser(Authentication authentication) {
        return Map.of("role", roleOf(authentication).name());
    }

    private Role roleOf(Authentication authentication) {
        Set<String> authorities = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        return Arrays.stream(Role.values())
                .filter(role -> authorities.contains(role.authority()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no known role"));
    }
}
