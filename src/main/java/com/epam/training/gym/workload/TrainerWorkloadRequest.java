package com.epam.training.gym.workload;

import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TrainerWorkloadRequest(
        String trainerUsername,
        String trainerFirstName,
        String trainerLastName,
        @JsonProperty("isActive") boolean isActive,
        LocalDate trainingDate,
        int trainingDuration,
        ActionType actionType) {

    public static TrainerWorkloadRequest of(Training training, ActionType actionType) {
        User trainer = training.getTrainer().getUser();
        return new TrainerWorkloadRequest(trainer.getUsername(), trainer.getFirstName(), trainer.getLastName(),
                trainer.isActive(), training.getTrainingDate(), training.getTrainingDuration(), actionType);
    }
}
