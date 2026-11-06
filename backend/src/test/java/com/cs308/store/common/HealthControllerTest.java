package com.cs308.store.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cs308.store.auth.JwtService;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.UserRepository;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = { JwtService.class, UserRepository.class })
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicAndReportsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
