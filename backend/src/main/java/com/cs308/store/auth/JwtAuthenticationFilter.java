package com.cs308.store.auth;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every request and logs the caller in if the Authorization header holds a valid
 * token. It maps the token's role to a Spring Security authority. It never rejects anything
 * itself: without a login, SecurityConfig's rules answer 401.
 *
 * Created in SecurityConfig instead of being a @Component, because Spring Boot would also
 * register a Filter bean outside the security chain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        bearerToken(request)
                .flatMap(jwtService::readClaims)
                .ifPresent(this::logIn);
        chain.doFilter(request, response);
    }

    private Optional<String> bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }

    /** The principal is the user id; controllers read it with @AuthenticationPrincipal Long. */
    private void logIn(TokenClaims claims) {
        var authorities = List.of(new SimpleGrantedAuthority(claims.role().authority()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(claims.userId(), null, authorities));
        SecurityContextHolder.setContext(context);
    }
}
