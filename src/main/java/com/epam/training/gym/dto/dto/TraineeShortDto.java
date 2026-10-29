package com.epam.training.gym.dto.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short trainee info, as shown inside a trainer's profile")
public record TraineeShortDto(
        String username,
        String firstName,
        String lastName) {
}