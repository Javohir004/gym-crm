package security;

import com.epam.training.gym.security.JwtService;
import com.epam.training.gym.security.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import support.MutableClock;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-0123456789";

    private MutableClock clock;
    private JwtService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-28T10:00:00Z"));
        service = new JwtService(SECRET, 60, clock);
    }

    @Test
    void generatedToken_canBeParsed_andCarriesUsernameAndExpiry() {
        String token = service.generateToken("John.Doe");

        Optional<TokenClaims> claims = service.parse(token);

        assertTrue(claims.isPresent());
        assertEquals("John.Doe", claims.get().username());
        assertFalse(claims.get().tokenId().isBlank());
        assertEquals(Instant.parse("2026-10-28T11:00:00Z"), claims.get().expiresAt());
    }

    @Test
    void everyToken_getsItsOwnId() {
        TokenClaims first = service.parse(service.generateToken("John.Doe")).orElseThrow();
        TokenClaims second = service.parse(service.generateToken("John.Doe")).orElseThrow();

        assertNotEquals(first.tokenId(), second.tokenId());
    }

    @Test
    void parse_rejectsExpiredToken() {
        String token = service.generateToken("John.Doe");

        clock.advance(Duration.ofMinutes(61));

        assertTrue(service.parse(token).isEmpty());
    }

    @Test
    void parse_acceptsTokenJustBeforeExpiry() {
        String token = service.generateToken("John.Doe");

        clock.advance(Duration.ofMinutes(59));

        assertTrue(service.parse(token).isPresent());
    }

    @Test
    void parse_rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService("another-secret-that-is-long-enough-9876543210", 60, clock);

        assertTrue(service.parse(other.generateToken("John.Doe")).isEmpty());
    }

    @Test
    void parse_rejectsTamperedToken() {
        String token = service.generateToken("John.Doe");
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertTrue(service.parse(tampered).isEmpty());
    }

    @Test
    void parse_rejectsGarbage() {
        assertTrue(service.parse("not-a-jwt").isEmpty());
        assertTrue(service.parse("").isEmpty());
    }

    @Test
    void constructor_rejectsTooShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60, clock));
    }
}