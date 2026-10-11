package com.epam.training.gym.util;

import com.epam.training.gym.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


public final class BasicAuthUtil {

    private BasicAuthUtil() {
    }

    public record Credentials(String username, String password) {
    }

    public static Credentials extract(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            throw new AuthenticationException("Missing or invalid Authorization header");
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6)), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator < 0) {
                throw new AuthenticationException("Invalid Authorization header");
            }
            return new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Invalid Authorization header");
        }
    }
}