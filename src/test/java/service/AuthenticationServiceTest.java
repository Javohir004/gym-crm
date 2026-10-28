package service;

import ch.qos.logback.classic.Level;
import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.TooManyAttemptsException;
import com.epam.training.gym.security.JwtService;
import com.epam.training.gym.security.LoginAttemptService;
import com.epam.training.gym.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import support.LogCapture;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(authenticationManager, jwtService, loginAttemptService);
    }

    @Test
    void login_returnsToken_andClearsFailedAttempts_whenCredentialsAreValid() {
        when(loginAttemptService.remainingBlock("John.Doe")).thenReturn(Duration.ZERO);
        when(jwtService.generateToken("John.Doe")).thenReturn("jwt-token");

        String token = service.login("John.Doe", "secret12345");

        assertEquals("jwt-token", token);
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("John.Doe", "secret12345"));
        verify(loginAttemptService).recordSuccess("John.Doe");
        verify(loginAttemptService, never()).recordFailure(any());
    }

    @Test
    void login_countsFailure_andThrowsAuthentication_whenPasswordIsWrong() {
        when(loginAttemptService.remainingBlock("John.Doe")).thenReturn(Duration.ZERO);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.login("John.Doe", "wrong"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(loginAttemptService).recordFailure("John.Doe");
        verify(loginAttemptService, never()).recordSuccess(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_throwsTooManyAttempts_withoutCheckingPassword_whenUserIsBlocked() {
        when(loginAttemptService.remainingBlock("John.Doe")).thenReturn(Duration.ofMinutes(4).plusSeconds(10));

        TooManyAttemptsException ex = assertThrows(TooManyAttemptsException.class,
                () -> service.login("John.Doe", "secret12345"));

        assertTrue(ex.getMessage().contains("5 minute"));
        verifyNoInteractions(authenticationManager, jwtService);
        verify(loginAttemptService, never()).recordFailure(any());
    }

    @Test
    void login_rejectsMissingCredentials_withoutTouchingAnything() {
        assertThrows(AuthenticationException.class, () -> service.login(" ", "secret12345"));
        assertThrows(AuthenticationException.class, () -> service.login("John.Doe", null));
        assertThrows(AuthenticationException.class, () -> service.login(null, ""));

        verifyNoInteractions(authenticationManager, jwtService, loginAttemptService);
    }

    @Test
    void login_logsOutcome_withoutLeakingPasswordOrToken() {
        when(loginAttemptService.remainingBlock("John.Doe")).thenReturn(Duration.ZERO);
        when(jwtService.generateToken("John.Doe")).thenReturn("jwt-token-value");

        try (LogCapture logs = new LogCapture(AuthenticationService.class)) {
            service.login("John.Doe", "secret12345");

            assertTrue(logs.messages(Level.INFO).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream()
                    .noneMatch(m -> m.contains("secret12345") || m.contains("jwt-token-value")));
        }
    }

    @Test
    void login_logsFailureAsWarning_withoutLeakingPassword() {
        when(loginAttemptService.remainingBlock("John.Doe")).thenReturn(Duration.ZERO);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        try (LogCapture logs = new LogCapture(AuthenticationService.class)) {
            assertThrows(AuthenticationException.class, () -> service.login("John.Doe", "wrongPass99"));

            assertTrue(logs.messages(Level.WARN).stream().anyMatch(m -> m.contains("John.Doe")));
            assertTrue(logs.allMessages().stream().noneMatch(m -> m.contains("wrongPass99")));
        }
    }

    @Test
    void issueToken_delegatesToJwtService() {
        when(jwtService.generateToken("John.Doe")).thenReturn("jwt-token");

        assertEquals("jwt-token", service.issueToken("John.Doe"));
    }
}