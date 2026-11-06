package com.cs308.store.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Creates and checks login tokens. A token holds only the user id: the role is read from the
 * database on every request, so a role change takes effect at once.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        // Throws at startup if the secret is shorter than 32 bytes.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public String createToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id, or empty if the token is expired, altered or not a token at all. */
    public Optional<Long> readUserId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.of(Long.valueOf(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
