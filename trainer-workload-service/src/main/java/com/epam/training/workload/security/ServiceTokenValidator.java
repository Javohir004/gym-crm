package com.epam.training.workload.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class ServiceTokenValidator {

    static final String SCOPE_CLAIM = "scope";
    static final String REQUIRED_SCOPE = "workload";

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenValidator.class);

    private final SecretKey key;
    private final String expectedIssuer;

    public ServiceTokenValidator(@Value("${workload.security.jwt.secret}") String secret,
                                 @Value("${workload.security.jwt.issuer:gym-crm}") String expectedIssuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expectedIssuer = expectedIssuer;
    }

    public Optional<String> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(expectedIssuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!REQUIRED_SCOPE.equals(claims.get(SCOPE_CLAIM, String.class)) || claims.getSubject() == null) {
                log.warn("Rejected service token: missing subject or scope");
                return Optional.empty();
            }
            return Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected service token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
