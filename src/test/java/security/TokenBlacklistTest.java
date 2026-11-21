package security;

import com.epam.training.gym.security.TokenBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.MutableClock;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistTest {

    private static final Instant START = Instant.parse("2026-10-28T10:00:00Z");

    private MutableClock clock;
    private TokenBlacklist blacklist;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        blacklist = new TokenBlacklist(clock);
    }

    @Test
    void unknownToken_isNotRevoked() {
        assertFalse(blacklist.isRevoked("some-id"));
    }

    @Test
    void revokedToken_isRejected_untilItExpires() {
        blacklist.revoke("token-1", START.plus(Duration.ofHours(1)));

        assertTrue(blacklist.isRevoked("token-1"));
        assertFalse(blacklist.isRevoked("token-2"));
    }

    @Test
    void revokedToken_isForgotten_afterItsExpiry() {
        blacklist.revoke("token-1", START.plus(Duration.ofHours(1)));

        clock.advance(Duration.ofHours(1));

        assertFalse(blacklist.isRevoked("token-1"));
    }

    @Test
    void revoke_purgesEntriesThatAlreadyExpired() {
        blacklist.revoke("old", START.plus(Duration.ofMinutes(10)));
        clock.advance(Duration.ofMinutes(30));

        blacklist.revoke("fresh", clock.instant().plus(Duration.ofMinutes(10)));

        assertFalse(blacklist.isRevoked("old"));
        assertTrue(blacklist.isRevoked("fresh"));
    }
}