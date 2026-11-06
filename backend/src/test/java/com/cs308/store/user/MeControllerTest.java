package com.cs308.store.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cs308.store.auth.JwtService;
import com.cs308.store.config.SecurityConfig;

/** Uses the real UserService, so these tests see what actually happens to the User object. */
@WebMvcTest(MeController.class)
@Import({ SecurityConfig.class, UserService.class })
@MockitoBean(types = JwtService.class)
class MeControllerTest {

    private static final long USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository users;

    private User existingUser(Role role) {
        User user = new User("Ada Lovelace", "ada@example.com", "a-bcrypt-hash", role, "Istanbul");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(users.findById(USER_ID)).willReturn(Optional.of(user));
        return user;
    }

    /** Logs in the same way JwtAuthenticationFilter does: the principal is the user id. */
    private static RequestPostProcessor loggedIn(Role role) {
        return authentication(new UsernamePasswordAuthenticationToken(USER_ID, null,
                List.of(new SimpleGrantedAuthority(role.authority()))));
    }

    private ResultActions patchProfile(Role role, String body) throws Exception {
        return mockMvc.perform(patch("/api/me").with(loggedIn(role))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void withoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheProfileOfRequirement14() throws Exception {
        existingUser(Role.CUSTOMER);

        mockMvc.perform(get("/api/me").with(loggedIn(Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.homeAddress").value("Istanbul"));
    }

    @Test
    void neverReturnsThePasswordHash() throws Exception {
        existingUser(Role.CUSTOMER);

        mockMvc.perform(get("/api/me").with(loggedIn(Role.CUSTOMER)))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void returnsTheRoleOfTheLoggedInUser(Role role) throws Exception {
        existingUser(role);

        mockMvc.perform(get("/api/me").with(loggedIn(role)))
                .andExpect(jsonPath("$.role").value(role.name()));
    }

    @Test
    void updatesNameAndAddress() throws Exception {
        User user = existingUser(Role.CUSTOMER);

        patchProfile(Role.CUSTOMER, """
                {"fullName": "  Ada King  ", "homeAddress": "Ankara"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ada King"));

        assertThat(user.getFullName()).isEqualTo("Ada King");
        assertThat(user.getHomeAddress()).isEqualTo("Ankara");
    }

    @Test
    void leftOutFieldsKeepTheirValue() throws Exception {
        User user = existingUser(Role.CUSTOMER);

        patchProfile(Role.CUSTOMER, """
                {"homeAddress": "Ankara"}
                """)
                .andExpect(status().isOk());

        assertThat(user.getFullName()).isEqualTo("Ada Lovelace");
    }

    // The acceptance criterion of SCRUM-11.
    @Test
    void customerCannotChangeTheirOwnRole() throws Exception {
        User user = existingUser(Role.CUSTOMER);

        patchProfile(Role.CUSTOMER, """
                {"fullName": "Ada", "role": "SALES_MANAGER"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void blankNameReturns400() throws Exception {
        existingUser(Role.CUSTOMER);

        patchProfile(Role.CUSTOMER, """
                {"fullName": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());
    }
}
