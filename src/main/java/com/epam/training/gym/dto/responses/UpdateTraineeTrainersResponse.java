package com.epam.training.gym.dto.responses;

import com.epam.training.gym.dto.TrainerShortDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The trainee's trainer list after the update")
public record UpdateTraineeTrainersResponse(List<TrainerShortDto> trainers) {
}