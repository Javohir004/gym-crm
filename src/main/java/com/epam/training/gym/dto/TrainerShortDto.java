package com.epam.training.gym.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short trainer info, as shown inside a trainee-related response")
public record TrainerShortDto(
        String username,
        String firstName,
        String lastName,
        @Schema(description = "Training type name") String specialization) {
}