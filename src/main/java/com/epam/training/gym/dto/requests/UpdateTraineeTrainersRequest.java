package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The full new list of trainers for a trainee")
public record UpdateTraineeTrainersRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String traineeUsername,

        @Schema(description = "Trainer usernames - this replaces the trainee's whole trainer list",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> trainers) {
}