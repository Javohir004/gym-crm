package com.epam.training.gym.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Fields needed to register a new Trainee")
public record TraineeRegistrationRequest(

        @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED, example = "John")
        String firstName,

        @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED, example = "Doe")
        String lastName,

        @Schema(description = "Date of birth", example = "2000-01-15")
        LocalDate dateOfBirth,

        @Schema(description = "Home address", example = "Tashkent, Chilonzor")
        String address) {
}