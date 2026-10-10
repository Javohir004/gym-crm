package com.epam.training.gym.dto.requests;

import java.time.LocalDate;


public record TrainingRequest(String traineeUsername,
                              String trainerUsername,
                              String trainingName,
                              String trainingTypeName,
                              LocalDate trainingDate,
                              Integer durationMinutes) {
}