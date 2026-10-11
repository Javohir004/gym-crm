package com.epam.training.gym.dto.responses;

import com.epam.training.gym.dto.TrainerShortDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Trainee's own profile")
public record TraineeProfileResponse(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean isActive,
        List<TrainerShortDto> trainers) {
}