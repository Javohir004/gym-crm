package com.epam.training.gym.security;

import java.time.Instant;

/** The few facts we read out of a verified JWT. */
public record TokenClaims(String tokenId, String username, Instant expiresAt) {
}