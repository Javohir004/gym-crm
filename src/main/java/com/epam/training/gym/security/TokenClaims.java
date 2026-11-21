package com.epam.training.gym.security;

import java.time.Instant;

public record TokenClaims(String tokenId, String username, Instant expiresAt) {
}