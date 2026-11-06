package com.cs308.store.auth;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.MeController;
import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;
import com.cs308.store.user.UserService;

/** SCRUM-10 and SCRUM-11 working together: a real token, through the whole security chain. */
@WebMvcTest(controllers = { MeController.class, BearerTokenAccessTest.SalesEndpoint.class }, properties = {
        "app.jwt.secret=test-secret-that-is-long-enough-for-hs256",
        "app.jwt.expiration-minutes=60" })
@Import({ SecurityConfig.class, JwtService.class, UserService.class, BearerTokenAccessTest.SalesEndpoint.class })
class BearerTokenAccessTest {

    private static final long USER_ID = 7L;

    @RestController
    static class SalesEndpoint {

        @GetMapping("/api/sales/ping")
        String ping() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository users;

    private String tokenFor(Role role) {
        User user = new User("Ada", "ada@example.com", "a-bcrypt-hash", role, null);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(users.findById(USER_ID)).willReturn(Optional.of(user));
        return "Bearer " + jwtService.createToken(USER_ID);
    }

    @Test
    void validTokenReachesProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", tokenFor(Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void forgedTokenReturns401() throws Exception {
        String forged = new JwtService("another-secret-that-is-long-enough-for-hs256", 60).createToken(USER_ID);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerTokenCannotEnterTheSalesArea() throws Exception {
        mockMvc.perform(get("/api/sales/ping").header("Authorization", tokenFor(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesManagerTokenEntersTheSalesArea() throws Exception {
        mockMvc.perform(get("/api/sales/ping").header("Authorization", tokenFor(Role.SALES_MANAGER)))
                .andExpect(status().isOk());
    }
}
