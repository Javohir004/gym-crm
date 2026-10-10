package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Fields needed to add a new training")
public record AddTrainingRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String traineeUsername,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String trainerUsername,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String trainingName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate trainingDate,
        @Schema(description = "Minutes", requiredMode = Schema.RequiredMode.REQUIRED) Integer trainingDuration) {
}