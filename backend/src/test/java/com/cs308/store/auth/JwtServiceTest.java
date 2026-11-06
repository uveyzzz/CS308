package com.cs308.store.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.security.WeakKeyException;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void readsBackTheUserIdFromItsOwnToken() {
        String token = jwtService.createToken(42L);

        assertThat(jwtService.readUserId(token)).contains(42L);
    }

    @Test
    void rejectsAnExpiredToken() {
        String expired = new JwtService(SECRET, -1).createToken(42L);

        assertThat(jwtService.readUserId(expired)).isEmpty();
    }

    @Test
    void rejectsATokenWhosePayloadWasChanged() {
        String[] parts = jwtService.createToken(42L).split("\\.");
        String otherPayload = jwtService.createToken(1L).split("\\.")[1];

        assertThat(jwtService.readUserId(parts[0] + "." + otherPayload + "." + parts[2])).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherSecret() {
        String forged = new JwtService("another-secret-that-is-long-enough-for-hs256", 60).createToken(42L);

        assertThat(jwtService.readUserId(forged)).isEmpty();
    }

    @Test
    void rejectsSomethingThatIsNotAToken() {
        assertThat(jwtService.readUserId("not-a-token")).isEmpty();
        assertThat(jwtService.readUserId("")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(WeakKeyException.class);
    }
}
