package com.epam.training.gym.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class TokenBlacklist {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenBlacklist(Clock clock) {
        this.clock = clock;
    }

    public void revoke(String tokenId, Instant expiresAt) {
        purgeExpired();
        revoked.put(tokenId, expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revoked.get(tokenId);
        return expiresAt != null && clock.instant().isBefore(expiresAt);
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        revoked.values().removeIf(expiresAt -> !now.isBefore(expiresAt));
    }
}