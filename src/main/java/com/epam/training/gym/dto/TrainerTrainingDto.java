package com.epam.training.gym.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "One training, as shown in a trainer's trainings list")
public record TrainerTrainingDto(
        String trainingName,
        LocalDate trainingDate,
        String trainingType,
        Integer trainingDuration,
        String traineeName) {
}