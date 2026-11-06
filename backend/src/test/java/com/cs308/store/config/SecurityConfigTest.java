package com.cs308.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cs308.store.auth.JwtService;
import com.cs308.store.common.HealthController;
import com.cs308.store.user.UserRepository;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = { JwtService.class, UserRepository.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsHashedNotStoredAsIs() {
        String hash = passwordEncoder.encode("secret123");

        assertThat(hash).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", hash)).isTrue();
        assertThat(passwordEncoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void samePasswordGetsDifferentHashes() {
        // Random salt: two users with the same password must not share a hash.
        assertThat(passwordEncoder.encode("secret123")).isNotEqualTo(passwordEncoder.encode("secret123"));
    }

    @Test
    void frontendDevServerPassesCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void unknownOriginIsRejectedByCors() throws Exception {
        mockMvc.perform(get("/api/health").header("Origin", "http://evil.example"))
                .andExpect(status().isForbidden());
    }
}
