package com.epam.training.gym.security;

import com.epam.training.gym.exception.AuthenticationException;
import com.epam.training.gym.exception.ForbiddenException;

import java.security.Principal;

public final class SelfAccess {

    private SelfAccess() {
    }

    public static void require(Principal principal, String pathUsername) {
        if (principal == null) {
            throw new AuthenticationException("Authentication required");
        }
        if (!principal.getName().equals(pathUsername)) {
            throw new ForbiddenException("You can only access your own profile");
        }
    }

    /** Same check, also for a username repeated in the request body (it is optional there). */
    public static void require(Principal principal, String pathUsername, String bodyUsername) {
        require(principal, pathUsername);
        if (bodyUsername != null && !bodyUsername.equals(pathUsername)) {
            throw new ForbiddenException("Username in the request does not match the URL");
        }
    }
}