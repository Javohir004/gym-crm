package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(example = "John.Doe") String username,
        @Schema(example = "aZ8kLp3qRt") String password) {
}