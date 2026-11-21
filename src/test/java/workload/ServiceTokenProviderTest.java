package workload;

import com.epam.training.gym.workload.ServiceTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import support.MutableClock;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ServiceTokenProviderTest {

    private static final String SECRET = "test-workload-secret-that-is-long-enough-0123";

    private final MutableClock clock = new MutableClock(Instant.now());
    private final ServiceTokenProvider provider = new ServiceTokenProvider(SECRET, 5, clock);

    @Test
    void token_isSignedWithTheSharedSecret_andCarriesIssuerAndScope() {
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(provider.getToken())
                .getPayload();

        assertEquals("gym-crm", claims.getIssuer());
        assertEquals("gym-crm", claims.getSubject());
        assertEquals("workload", claims.get("scope", String.class));
    }

    @Test
    void token_isReused_whileFarFromExpiry() {
        String first = provider.getToken();
        clock.advance(Duration.ofMinutes(4));

        assertEquals(first, provider.getToken());
    }

    @Test
    void token_isRenewed_shortlyBeforeExpiry() {
        String first = provider.getToken();
        clock.advance(Duration.ofMinutes(4).plusSeconds(45));

        assertNotEquals(first, provider.getToken());
    }
}
