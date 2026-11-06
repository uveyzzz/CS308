package com.cs308.store.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cs308.store.auth.JwtAuthenticationFilter;
import com.cs308.store.auth.JwtService;
import com.cs308.store.common.ApiError;
import com.cs308.store.user.Role;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Central security rules.
 *
 * The API is stateless: no server-side session, no login form. Each request proves
 * who it is with a token, which JwtAuthenticationFilter checks. A non-public endpoint
 * answers 401 without a valid token, and 403 if the user's role is not allowed.
 *
 * The first matching rule wins, so specific rules must stay above general ones.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler, JwtService jwtService) throws Exception {
        http
                // CSRF protection is for cookie-based sessions; a token-based API does not need it.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/**").permitAll()
                        // Guests can browse the catalog (requirement 4).
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        // Any other method changes the catalog, so only product managers may do it (requirement 12).
                        .requestMatchers("/api/products/**", "/api/categories/**").hasRole(Role.PRODUCT_MANAGER.name())
                        // Admin controllers also carry @PreAuthorize. These rules are the safety net:
                        // a forgotten annotation cannot open an admin endpoint (requirements 10, 17).
                        .requestMatchers("/api/admin/products/**").hasRole(Role.PRODUCT_MANAGER.name())
                        .requestMatchers("/api/admin/sales/**").hasRole(Role.SALES_MANAGER.name())
                        .requestMatchers("/api/admin/support/**").hasRole(Role.SUPPORT_AGENT.name())
                        .requestMatchers("/api/admin/**").denyAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * The security filter rejects a request before any controller runs, so GlobalExceptionHandler
     * never sees it. These two beans keep the 401 and 403 in the common ApiError format.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiError.of(HttpStatus.FORBIDDEN.value(), "Access denied"));
        };
    }

    /** BCrypt hashes with a random salt; never store a password any other way (requirement 17). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Lets the React dev server (another port) call the API from the browser. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
