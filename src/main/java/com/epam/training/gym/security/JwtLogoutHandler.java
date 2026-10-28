package com.epam.training.gym.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/** Logout: the token in the request is put on the blacklist, so it cannot be used again. */
@Component
public class JwtLogoutHandler implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtLogoutHandler.class);

    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    public JwtLogoutHandler(JwtService jwtService, TokenBlacklist tokenBlacklist) {
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        BearerToken.from(request)
                .flatMap(jwtService::parse)
                .ifPresentOrElse(claims -> {
                    tokenBlacklist.revoke(claims.tokenId(), claims.expiresAt());
                    log.info("User logged out: username={}", claims.username());
                }, () -> log.debug("Logout called without a valid token, nothing to revoke"));
    }
}