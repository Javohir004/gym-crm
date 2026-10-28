package security;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.security.JwtLogoutHandler;
import com.epam.training.gym.security.JwtService;
import com.epam.training.gym.security.TokenBlacklist;
import com.epam.training.gym.security.TokenClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import support.LogCapture;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtLogoutHandlerTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private TokenBlacklist tokenBlacklist;

    private JwtLogoutHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new JwtLogoutHandler(jwtService, tokenBlacklist);
        request = new MockHttpServletRequest("POST", "/api/logout");
    }

    @Test
    void logout_putsTheTokenOnTheBlacklist() {
        Instant expiry = Instant.parse("2026-10-28T11:00:00Z");
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtService.parse("good-token")).thenReturn(Optional.of(new TokenClaims("id-1", "John.Doe", expiry)));

        try (LogCapture logs = new LogCapture(JwtLogoutHandler.class)) {
            handler.logout(request, new MockHttpServletResponse(), null);

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("good-token")));
        }
        verify(tokenBlacklist).revoke("id-1", expiry);
    }

    @Test
    void logout_doesNothing_whenThereIsNoToken() {
        handler.logout(request, new MockHttpServletResponse(), null);

        verifyNoInteractions(jwtService, tokenBlacklist);
    }

    @Test
    void logout_doesNothing_whenTokenIsInvalid() {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.parse("bad-token")).thenReturn(Optional.empty());

        handler.logout(request, new MockHttpServletResponse(), null);

        verify(tokenBlacklist, never()).revoke(any(), any());
    }
}