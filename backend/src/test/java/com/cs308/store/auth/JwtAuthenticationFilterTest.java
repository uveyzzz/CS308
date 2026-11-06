package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cs308.store.user.Role;

import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {

    private static final long USER_ID = 7L;

    private final JwtService jwtService = new JwtService("test-secret-that-is-long-enough-for-hs256", 60);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

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
    void validTokenLogsTheUserInWithTheRoleFromTheToken() throws Exception {
        request.addHeader("Authorization", "Bearer " + jwtService.createToken(USER_ID, Role.SALES_MANAGER));

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
        request.addHeader("Authorization", "Basic " + jwtService.createToken(USER_ID, Role.CUSTOMER));

        assertThat(runFilter()).isNull();
    }

    @Test
    void invalidTokenLeavesTheRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer not-a-token");

        assertThat(runFilter()).isNull();
    }
}
