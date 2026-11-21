package com.epam.training.workload.dto;

import com.epam.training.workload.model.ActionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record WorkloadRequest(
        @NotBlank String trainerUsername,
        @NotBlank String trainerFirstName,
        @NotBlank String trainerLastName,
        @NotNull @JsonProperty("isActive") Boolean isActive,
        @NotNull LocalDate trainingDate,
        @NotNull @Positive Integer trainingDuration,
        @NotNull ActionType actionType) {
}
