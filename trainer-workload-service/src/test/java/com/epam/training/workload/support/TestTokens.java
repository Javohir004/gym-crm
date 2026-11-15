package com.epam.training.workload.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public final class TestTokens {

    public static final String SECRET = "local-dev-workload-secret-change-me-0123456789";

    private TestTokens() {
    }

    public static String valid() {
        return token(SECRET, "gym-crm", "workload", Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    public static String token(String secret, String issuer, String scope, Instant expiresAt) {
        return Jwts.builder()
                .subject("gym-crm")
                .issuer(issuer)
                .claim("scope", scope)
                .issuedAt(Date.from(expiresAt.minus(10, ChronoUnit.MINUTES)))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
