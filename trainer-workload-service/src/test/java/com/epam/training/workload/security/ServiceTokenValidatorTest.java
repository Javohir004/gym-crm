package com.epam.training.workload.security;

import com.epam.training.workload.support.TestTokens;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceTokenValidatorTest {

    private static final Instant IN_FIVE_MINUTES = Instant.now().plus(5, ChronoUnit.MINUTES);

    private final ServiceTokenValidator validator = new ServiceTokenValidator(TestTokens.SECRET, "gym-crm");

    @Test
    void validToken_returnsTheCallingService() {
        assertEquals(Optional.of("gym-crm"), validator.validate(TestTokens.valid()));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String token = TestTokens.token("another-secret-that-is-long-enough-0123456789", "gym-crm",
                "workload", IN_FIVE_MINUTES);

        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    void expiredToken_isRejected() {
        String token = TestTokens.token(TestTokens.SECRET, "gym-crm", "workload",
                Instant.now().minus(1, ChronoUnit.MINUTES));

        assertTrue(validator.validate(token).isEmpty());
    }

    @Test
    void tokenFromAnotherIssuer_isRejected() {
        assertTrue(validator.validate(TestTokens.token(TestTokens.SECRET, "someone-else", "workload",
                IN_FIVE_MINUTES)).isEmpty());
    }

    @Test
    void tokenWithoutWorkloadScope_isRejected() {
        assertTrue(validator.validate(TestTokens.token(TestTokens.SECRET, "gym-crm", "admin",
                IN_FIVE_MINUTES)).isEmpty());
    }

    @Test
    void garbage_isRejected() {
        assertTrue(validator.validate("not-a-jwt").isEmpty());
        assertTrue(validator.validate("").isEmpty());
    }
}
