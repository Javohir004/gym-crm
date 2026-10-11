package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fields needed to register a new Trainer")
public record TrainerRegistrationRequest(

        @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Anna")
        String firstName,

        @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Lee")
        String lastName,

        @Schema(description = "Specialization (an existing Training Type name)",
                requiredMode = Schema.RequiredMode.REQUIRED, example = "Yoga")
        String specialization) {
}