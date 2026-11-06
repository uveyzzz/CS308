package com.cs308.store.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cs308.store.user.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and checks login tokens. A token carries the user id and role, so a request needs no
 * database lookup to be authorized. The signature stops anyone from changing them. The cost is
 * that a role change only takes effect when the user logs in again or the token expires.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        // Throws at startup if the secret is shorter than 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public String createToken(long userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    /** Returns the token's claims, or empty if it is expired, altered or not a valid token. */
    public Optional<TokenClaims> readClaims(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
            String role = claims.get(ROLE_CLAIM, String.class);
            if (role == null) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(Long.parseLong(claims.getSubject()), Role.valueOf(role)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
