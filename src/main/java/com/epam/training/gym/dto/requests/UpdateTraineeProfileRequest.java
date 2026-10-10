package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Fields to update on a Trainee's profile")
public record UpdateTraineeProfileRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
        LocalDate dateOfBirth,
        String address,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Boolean isActive) {
}