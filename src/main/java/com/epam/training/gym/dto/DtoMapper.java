package com.epam.training.gym.dto;

import com.epam.training.gym.dto.responses.TraineeProfileResponse;
import com.epam.training.gym.dto.responses.TrainerProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTraineeProfileResponse;
import com.epam.training.gym.dto.responses.UpdateTrainerProfileResponse;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.model.TrainingType;

import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static TrainerShortDto toTrainerShortDto(Trainer trainer) {
        return new TrainerShortDto(
                trainer.getUser().getUsername(),
                trainer.getUser().getFirstName(),
                trainer.getUser().getLastName(),
                trainer.getSpecialization().getTrainingTypeName());
    }

    public static TraineeShortDto toTraineeShortDto(Trainee trainee) {
        return new TraineeShortDto(
                trainee.getUser().getUsername(),
                trainee.getUser().getFirstName(),
                trainee.getUser().getLastName());
    }

    public static List<TrainerShortDto> toTrainerShortDtoList(Iterable<Trainer> trainers) {
        return toList(trainers, DtoMapper::toTrainerShortDto);
    }

    public static List<TraineeShortDto> toTraineeShortDtoList(Iterable<Trainee> trainees) {
        return toList(trainees, DtoMapper::toTraineeShortDto);
    }

    public static TraineeProfileResponse toTraineeProfileResponse(Trainee trainee) {
        return new TraineeProfileResponse(
                trainee.getUser().getFirstName(),
                trainee.getUser().getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.getUser().isActive(),
                toTrainerShortDtoList(trainee.getTrainers()));
    }

    public static UpdateTraineeProfileResponse toUpdateTraineeProfileResponse(Trainee trainee) {
        return new UpdateTraineeProfileResponse(
                trainee.getUser().getUsername(),
                trainee.getUser().getFirstName(),
                trainee.getUser().getLastName(),
                trainee.getDateOfBirth(),
                trainee.getAddress(),
                trainee.getUser().isActive(),
                toTrainerShortDtoList(trainee.getTrainers()));
    }

    public static TrainerProfileResponse toTrainerProfileResponse(Trainer trainer) {
        return new TrainerProfileResponse(
                trainer.getUser().getFirstName(),
                trainer.getUser().getLastName(),
                trainer.getSpecialization().getTrainingTypeName(),
                trainer.getUser().isActive(),
                toTraineeShortDtoList(trainer.getTrainees()));
    }

    public static UpdateTrainerProfileResponse toUpdateTrainerProfileResponse(Trainer trainer) {
        return new UpdateTrainerProfileResponse(
                trainer.getUser().getUsername(),
                trainer.getUser().getFirstName(),
                trainer.getUser().getLastName(),
                trainer.getSpecialization().getTrainingTypeName(),
                trainer.getUser().isActive(),
                toTraineeShortDtoList(trainer.getTrainees()));
    }

    public static TraineeTrainingDto toTraineeTrainingDto(Training training) {
        return new TraineeTrainingDto(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getTrainingDuration(),
                training.getTrainer().getUser().getFirstName() + " " + training.getTrainer().getUser().getLastName());
    }

    public static TrainerTrainingDto toTrainerTrainingDto(Training training) {
        return new TrainerTrainingDto(
                training.getTrainingName(),
                training.getTrainingDate(),
                training.getTrainingType().getTrainingTypeName(),
                training.getTrainingDuration(),
                training.getTrainee().getUser().getFirstName() + " " + training.getTrainee().getUser().getLastName());
    }

    public static List<TraineeTrainingDto> toTraineeTrainingDtoList(List<Training> trainings) {
        return toList(trainings, DtoMapper::toTraineeTrainingDto);
    }

    public static List<TrainerTrainingDto> toTrainerTrainingDtoList(List<Training> trainings) {
        return toList(trainings, DtoMapper::toTrainerTrainingDto);
    }
                                                                      // shu yerda training type id ni id ga almashtirdim
    public static TrainingTypeDto toTrainingTypeDto(TrainingType trainingType) {
        return new TrainingTypeDto(trainingType.getTrainingTypeName(), trainingType.getId());
    }

    public static List<TrainingTypeDto> toTrainingTypeDtoList(List<TrainingType> types) {
        return toList(types, DtoMapper::toTrainingTypeDto);
    }

    private static <S, T> List<T> toList(Iterable<S> source, java.util.function.Function<S, T> mapper) {
        List<T> result = new java.util.ArrayList<>();
        for (S item : source) {
            result.add(mapper.apply(item));
        }
        return result;
    }
}