package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fields to update on a Trainer's profile (specialization is read-only and is ignored)")
public record UpdateTrainerProfileRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        @Schema(description = "Read-only, ignored if sent") String specialization,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isActive) {
}