package com.cs308.store.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.cs308.store.common.ConflictException;
import com.cs308.store.common.UnauthorizedException;
import com.cs308.store.config.SecurityConfig;
import com.cs308.store.user.Role;
import com.cs308.store.user.UserRepository;
import com.cs308.store.user.UserResponse;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@MockitoBean(types = { JwtService.class, UserRepository.class })
class AuthControllerTest {

    private static final String VALID_REGISTRATION = """
            {"fullName": "Ada Lovelace", "email": "ada@example.com", "password": "long-enough"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static AuthResponse response() {
        return new AuthResponse("a-token",
                new UserResponse(1L, "Ada Lovelace", "ada@example.com", Role.CUSTOMER, null));
    }

    @Test
    void registerIsPublicAndReturns201WithAToken() throws Exception {
        given(authService.register(any())).willReturn(response());

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("a-token"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void responseNeverContainsThePassword() throws Exception {
        given(authService.register(any())).willReturn(response());

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void invalidEmailReturns400WithTheField() throws Exception {
        postJson("/api/auth/register", """
                {"fullName": "Ada", "email": "not-an-email", "password": "long-enough"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void shortPasswordReturns400() throws Exception {
        postJson("/api/auth/register", """
                {"fullName": "Ada", "email": "ada@example.com", "password": "short"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void passwordOverTheBcryptLimitReturns400() throws Exception {
        // 40 characters, but 80 bytes: each Turkish letter here takes two bytes.
        String password = "ğ".repeat(40);

        postJson("/api/auth/register", """
                {"fullName": "Ada", "email": "ada@example.com", "password": "%s"}
                """.formatted(password))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.passwordShortEnough").exists());
    }

    @Test
    void takenEmailReturns409() throws Exception {
        given(authService.register(any())).willThrow(new ConflictException("This email is already registered"));

        postJson("/api/auth/register", VALID_REGISTRATION)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email is already registered"));
    }

    @Test
    void loginIsPublicAndReturnsAToken() throws Exception {
        given(authService.login(any())).willReturn(response());

        postJson("/api/auth/login", """
                {"email": "ada@example.com", "password": "long-enough"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a-token"));
    }

    @Test
    void wrongLoginReturns401WithTheCommonErrorFormat() throws Exception {
        given(authService.login(any())).willThrow(new UnauthorizedException("Invalid email or password"));

        postJson("/api/auth/login", """
                {"email": "ada@example.com", "password": "wrong-password"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void missingLoginFieldsReturn400() throws Exception {
        postJson("/api/auth/login", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        postJson("/api/auth/login", "{not json")
                .andExpect(status().isBadRequest());
    }
}
