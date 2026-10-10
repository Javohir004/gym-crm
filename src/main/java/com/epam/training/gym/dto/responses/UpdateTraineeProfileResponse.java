package com.epam.training.gym.dto.responses;

import com.epam.training.gym.dto.TrainerShortDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/** Response for "Update Trainee Profile" (endpoint 6). */
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