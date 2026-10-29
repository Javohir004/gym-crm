package com.epam.training.gym.dto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A training type")
public record TrainingTypeDto(
        String trainingType,
        Long trainingTypeId) {
}