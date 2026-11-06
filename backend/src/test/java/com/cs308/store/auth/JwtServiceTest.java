package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cs308.store.user.Role;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @ParameterizedTest
    @EnumSource(Role.class)
    void tokenCarriesTheUserIdAndRole(Role role) {
        String token = jwtService.createToken(42L, role);

        assertThat(jwtService.readClaims(token)).contains(new TokenClaims(42L, role));
    }

    @Test
    void rejectsAnExpiredToken() {
        String expired = new JwtService(SECRET, -1).createToken(42L, Role.CUSTOMER);

        assertThat(jwtService.readClaims(expired)).isEmpty();
    }

    @Test
    void rejectsATokenWhoseRoleWasRaised() {
        String[] customer = jwtService.createToken(42L, Role.CUSTOMER).split("\\.");
        String managerPayload = jwtService.createToken(42L, Role.SALES_MANAGER).split("\\.")[1];

        assertThat(jwtService.readClaims(customer[0] + "." + managerPayload + "." + customer[2])).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        String forged = new JwtService("another-secret-that-is-long-enough-for-hs256", 60)
                .createToken(42L, Role.SALES_MANAGER);

        assertThat(jwtService.readClaims(forged)).isEmpty();
    }

    @Test
    void rejectsATokenWithoutAKnownRole() {
        assertThat(jwtService.readClaims(signedToken(null))).isEmpty();
        assertThat(jwtService.readClaims(signedToken("ADMIN"))).isEmpty();
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThat(jwtService.readClaims("not-a-token")).isEmpty();
        assertThat(jwtService.readClaims("")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(WeakKeyException.class);
    }

    /** A correctly signed token whose role claim the service did not write itself. */
    private static String signedToken(String role) {
        return Jwts.builder()
                .subject("42")
                .claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
