package com.epam.training.gym.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated login credentials")
public record RegistrationResponse(

        @Schema(description = "Generated username", example = "John.Doe")
        String username,

        @Schema(description = "Generated password", example = "aZ8kLp3qRt")
        String password) {
}