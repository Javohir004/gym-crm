package security;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.LogCapture;
import support.MutableClock;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-28T10:00:00Z"));
        service = new LoginAttemptService(3, 5, clock);
    }

    private void fail(String username, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(username);
        }
    }

    @Test
    void user_isNotBlocked_beforeAnyFailure() {
        assertFalse(service.isBlocked("John.Doe"));
        assertEquals(Duration.ZERO, service.remainingBlock("John.Doe"));
    }

    @Test
    void user_isNotBlocked_afterTwoFailures() {
        fail("John.Doe", 2);

        assertFalse(service.isBlocked("John.Doe"));
    }

    @Test
    void user_isBlockedForFiveMinutes_afterThirdFailure() {
        fail("John.Doe", 3);

        assertTrue(service.isBlocked("John.Doe"));
        assertEquals(Duration.ofMinutes(5), service.remainingBlock("John.Doe"));
    }

    @Test
    void remainingBlock_shrinksAsTimePasses() {
        fail("John.Doe", 3);

        clock.advance(Duration.ofMinutes(2));

        assertEquals(Duration.ofMinutes(3), service.remainingBlock("John.Doe"));
    }

    @Test
    void user_isUnblocked_afterFiveMinutes_andStartsWithCleanSlate() {
        fail("John.Doe", 3);

        clock.advance(Duration.ofMinutes(5));

        assertFalse(service.isBlocked("John.Doe"));
        fail("John.Doe", 2);
        assertFalse(service.isBlocked("John.Doe"));
    }

    @Test
    void firstFailureAfterBlockExpired_startsNewCountInsteadOfBlockingAgain() {
        fail("John.Doe", 3);
        clock.advance(Duration.ofMinutes(6));

        service.recordFailure("John.Doe");

        assertFalse(service.isBlocked("John.Doe"));
    }

    @Test
    void success_resetsTheFailureCounter() {
        fail("John.Doe", 2);

        service.recordSuccess("John.Doe");
        fail("John.Doe", 2);

        assertFalse(service.isBlocked("John.Doe"));
    }

    @Test
    void blocking_isPerUsername() {
        fail("John.Doe", 3);

        assertTrue(service.isBlocked("John.Doe"));
        assertFalse(service.isBlocked("Anna.Lee"));
    }

    @Test
    void blocking_isLoggedAsWarning() {
        try (LogCapture logs = new LogCapture(LoginAttemptService.class)) {
            fail("John.Doe", 3);

            assertTrue(logs.messages(Level.WARN).stream()
                    .anyMatch(m -> m.contains("John.Doe") && m.contains("blocked")));
        }
    }
}