package com.cs308.store.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.cs308.store.auth.JwtAuthenticationFilter;
import com.cs308.store.auth.JwtService;
import com.cs308.store.common.ApiError;
import com.cs308.store.user.Role;
import com.cs308.store.user.UserRepository;
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
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AccessDeniedHandler accessDeniedHandler,
            JwtService jwtService, UserRepository userRepository) throws Exception {
        http
                // CSRF protection is for cookie-based sessions; a token-based API does not need it.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/auth/**").permitAll()
                        // Guests can browse the catalog (requirement 4).
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                        // Any other method changes the catalog, so only product managers may do it (requirement 12).
                        .requestMatchers("/api/products/**", "/api/categories/**").hasRole(Role.PRODUCT_MANAGER.name())
                        // Each management area belongs to one role, so a new endpoint under these
                        // paths is protected without any extra code (requirements 10, 17).
                        .requestMatchers("/api/sales/**").hasRole(Role.SALES_MANAGER.name())
                        .requestMatchers("/api/product-manager/**").hasRole(Role.PRODUCT_MANAGER.name())
                        .requestMatchers("/api/support/**").hasRole(Role.SUPPORT_AGENT.name())
                        .anyRequest().authenticated());
        return http.build();
    }

    /**
     * A role rule rejects the request in the security filter, before any controller runs, so
     * GlobalExceptionHandler never sees it. This keeps the 403 in the common ApiError format.
     */
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
