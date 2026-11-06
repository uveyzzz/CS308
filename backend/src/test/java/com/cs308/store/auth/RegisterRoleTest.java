package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

/** Uses the real controller, service and token code: only the database is replaced. */
@WebMvcTest(controllers = AuthController.class, properties = {
        "app.jwt.secret=test-secret-that-is-long-enough-for-hs256",
        "app.jwt.expiration-minutes=60" })
@Import({ SecurityConfig.class, AuthService.class, JwtService.class })
class RegisterRoleTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository users;

    private MvcResult registerAskingFor(String role) throws Exception {
        given(users.saveAndFlush(any(User.class))).willAnswer(call -> {
            User user = call.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 5L);
            return user;
        });
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"fullName": "Mallory", "email": "m@example.com", "password": "long-enough", "role": "%s"}
                """.formatted(role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andReturn();
    }

    @ParameterizedTest
    @ValueSource(strings = { "SALES_MANAGER", "PRODUCT_MANAGER", "SUPPORT_AGENT" })
    void aCustomerCannotBecomeStaffThroughRegister(String role) throws Exception {
        registerAskingFor(role);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void theTokenFromRegisterCarriesTheCustomerRole() throws Exception {
        MvcResult result = registerAskingFor("SALES_MANAGER");

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertThat(jwtService.readClaims(token)).contains(new TokenClaims(5L, Role.CUSTOMER));
    }
}
