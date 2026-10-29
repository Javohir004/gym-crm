package com.epam.training.gym.dto.responses;

import com.epam.training.gym.dto.dto.TrainerShortDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Trainee's profile after the update")
public record UpdateTraineeProfileResponse(
        String username,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String address,
        boolean isActive,
        List<TrainerShortDto> trainers) {
}