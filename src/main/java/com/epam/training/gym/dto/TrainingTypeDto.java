package com.epam.training.gym.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** One row of "Get Training types" (endpoint 17). */
@Schema(description = "A training type")
public record TrainingTypeDto(
        String trainingType,
        Long trainingTypeId) {
}