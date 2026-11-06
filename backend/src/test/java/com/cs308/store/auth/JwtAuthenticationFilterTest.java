package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cs308.store.user.Role;
import com.cs308.store.user.User;
import com.cs308.store.user.UserRepository;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    private static final long USER_ID = 7L;

    private final JwtService jwtService = new JwtService("test-secret-that-is-long-enough-for-hs256", 60);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, users);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void userExists() {
        User user = new User("Ada", "ada@example.com", "a-bcrypt-hash", Role.SALES_MANAGER, null);
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(users.findById(USER_ID)).willReturn(Optional.of(user));
    }

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private Authentication runFilter() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response); // the request always continues
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void validTokenLogsTheUserInWithTheirRole() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtService.createToken(USER_ID));

        Authentication login = runFilter();

        assertThat(login.getPrincipal()).isEqualTo(USER_ID);
        assertThat(login.getAuthorities()).extracting("authority").containsExactly("ROLE_SALES_MANAGER");
    }

    @Test
    void noHeaderLeavesTheRequestAnonymous() throws Exception {
        assertThat(runFilter()).isNull();
    }

    @Test
    void anotherAuthorizationSchemeIsIgnored() throws Exception {
        request.addHeader("Authorization", "Basic " + jwtService.createToken(USER_ID));

        assertThat(runFilter()).isNull();
    }

    @Test
    void invalidTokenLeavesTheRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-token");

        assertThat(runFilter()).isNull();
    }

    @Test
    void tokenOfADeletedUserLeavesTheRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtService.createToken(99L));
        given(users.findById(99L)).willReturn(Optional.empty());

        assertThat(runFilter()).isNull();
    }
}
