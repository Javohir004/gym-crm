package com.epam.training.gym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    public JwtService(@Value("${gym.security.jwt.secret}") String secret,
                      @Value("${gym.security.jwt.expiration-minutes:60}") long expirationMinutes,
                      Clock clock) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("gym.security.jwt.secret must be at least "
                    + MIN_SECRET_BYTES + " characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.lifetime = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    public String generateToken(String username) {
        Instant now = clock.instant();
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key)
                .compact();
        log.debug("JWT issued for username={}, valid for {} minute(s)", username, lifetime.toMinutes());
        return token;
    }

    /** Returns the claims, or empty if the token is malformed, tampered with or expired. */
    public Optional<TokenClaims> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims.getId() == null || claims.getSubject() == null || claims.getExpiration() == null) {
                log.debug("Rejected JWT: required claims are missing");
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(claims.getId(), claims.getSubject(),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }
}