package com.epam.training.gym.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token")
public record LoginResponse(
        @Schema(description = "Send it in every request as 'Authorization: Bearer <token>'")
        String token) {
}