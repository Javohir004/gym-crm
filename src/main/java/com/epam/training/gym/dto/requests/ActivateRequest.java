package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The desired active state")
public record ActivateRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isActive) {
}