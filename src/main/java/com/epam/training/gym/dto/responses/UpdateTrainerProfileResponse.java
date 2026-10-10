package com.epam.training.gym.dto.responses;

import com.epam.training.gym.dto.TraineeShortDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Trainer's profile after the update")
public record UpdateTrainerProfileResponse(
        String username,
        String firstName,
        String lastName,
        String specialization,
        boolean isActive,
        List<TraineeShortDto> trainees) {
}