package com.epam.training.gym.service;

import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.TooManyAttemptsException;
import com.epam.training.gym.security.JwtService;
import com.epam.training.gym.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Login (username + password -> JWT) with brute-force protection. */
@Service
public class AuthenticationService {

    static final String INVALID_CREDENTIALS = "Invalid username or password";

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 JwtService jwtService,
                                 LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Login rejected: username or password is missing");
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        rejectIfBlocked(username);

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.recordFailure(username);
            log.warn("Login failed for username={}", username);
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        loginAttemptService.recordSuccess(username);
        log.info("Login succeeded for username={}", username);
        return jwtService.generateToken(username);
    }

    /** Token for a user that has just been created (registration returns it right away). */
    public String issueToken(String username) {
        return jwtService.generateToken(username);
    }

    private void rejectIfBlocked(String username) {
        Duration remaining = loginAttemptService.remainingBlock(username);
        if (remaining.isPositive()) {
            long minutes = Math.max(1, remaining.plusSeconds(59).toMinutes());
            log.warn("Login rejected: username={} is blocked for another {} minute(s)", username, minutes);
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in " + minutes + " minute(s)");
        }
    }
}