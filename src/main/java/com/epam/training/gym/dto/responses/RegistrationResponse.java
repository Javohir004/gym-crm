package com.epam.training.gym.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated login credentials and a ready-to-use access token")
public record RegistrationResponse(

        @Schema(description = "Generated username", example = "John.Doe")
        String username,

        @Schema(description = "Generated password (shown only now, the server keeps just its hash)", example = "aZ8kLp3qRt")
        String password,

        @Schema(description = "JWT access token, send it as 'Authorization: Bearer <token>'")
        String token) {
}