package com.epam.training.gym.service;


/**
 * Username and plain-text password of a newly registered profile. Only the BCrypt hash is stored,
 * so this is the single moment the plain password exists and can be returned to the client.
 */
public record GeneratedCredentials(String username, String password) {

    @Override
    public String toString() {
        return "GeneratedCredentials[username=" + username + "]";
    }
}
