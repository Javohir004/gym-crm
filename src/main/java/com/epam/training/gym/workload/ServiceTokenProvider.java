package com.epam.training.gym.workload;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class ServiceTokenProvider {

    static final String ISSUER = "gym-crm";
    static final String SCOPE_CLAIM = "scope";
    static final String SCOPE = "workload";
    private static final Duration RENEW_BEFORE_EXPIRY = Duration.ofSeconds(30);
    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    private final SecretKey key;
    private final Duration lifetime;
    private final Clock clock;

    private String token;
    private Instant expiresAt;

    public ServiceTokenProvider(@Value("${gym.workload.jwt.secret}") String secret,
                                @Value("${gym.workload.jwt.expiration-minutes:5}") long expirationMinutes,
                                Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    public synchronized String getToken() {
        Instant now = clock.instant();
        if (token == null || now.isAfter(expiresAt.minus(RENEW_BEFORE_EXPIRY))) {
            expiresAt = now.plus(lifetime);
            token = Jwts.builder()
                    .subject(ISSUER)
                    .issuer(ISSUER)
                    .claim(SCOPE_CLAIM, SCOPE)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiresAt))
                    .signWith(key)
                    .compact();
            log.debug("Issued a new service token for trainer-workload-service, valid until {}", expiresAt);
        }
        return token;
    }
}
