package com.epam.training.gym.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private record Attempts(int failures, Instant blockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration blockDuration;
    private final Clock clock;

    public LoginAttemptService(@Value("${gym.security.login.max-attempts:3}") int maxAttempts,
                               @Value("${gym.security.login.block-minutes:5}") long blockMinutes,
                               Clock clock) {
        this.maxAttempts = maxAttempts;
        this.blockDuration = Duration.ofMinutes(blockMinutes);
        this.clock = clock;
    }

    public boolean isBlocked(String username) {
        return remainingBlock(username).isPositive();
    }

    public Duration remainingBlock(String username) {
        Attempts current = attempts.get(username);
        if (current == null || current.blockedUntil() == null) {
            return Duration.ZERO;
        }
        Instant now = clock.instant();
        if (now.isBefore(current.blockedUntil())) {
            return Duration.between(now, current.blockedUntil());
        }
        attempts.remove(username, current);
        return Duration.ZERO;
    }

    public void recordFailure(String username) {
        attempts.compute(username, (name, current) -> {
            Instant now = clock.instant();
            boolean blockOver = current != null && current.blockedUntil() != null
                    && !now.isBefore(current.blockedUntil());
            int failures = (current == null || blockOver ? 0 : current.failures()) + 1;
            if (failures >= maxAttempts) {
                log.warn("Username={} blocked for {} minute(s) after {} failed login attempts",
                        username, blockDuration.toMinutes(), failures);
                return new Attempts(failures, now.plus(blockDuration));
            }
            log.debug("Failed login attempt {} of {} for username={}", failures, maxAttempts, username);
            return new Attempts(failures, null);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }
}