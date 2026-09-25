package com.epam.training.gym.facade;

import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.service.TraineeService;
import com.epam.training.gym.service.TrainerService;
import com.epam.training.gym.service.TrainingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;


@Component
public class GymFacade {

    private static final Logger log = LoggerFactory.getLogger(GymFacade.class);

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;

    public GymFacade(TraineeService traineeService,
                     TrainerService trainerService,
                     TrainingService trainingService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        log.debug("GymFacade initialized");
    }



    public Trainee createTrainee(Trainee trainee) {
        log.info("Facade: createTrainee");
        return traineeService.create(trainee);
    }

    public Trainee updateTrainee(Trainee trainee) {
        log.info("Facade: updateTrainee id={}", trainee == null ? null : trainee.getUserId());
        return traineeService.update(trainee);
    }

    public void deleteTrainee(Long id) {
        log.info("Facade: deleteTrainee id={}", id);
        traineeService.delete(id);
    }

    public Optional<Trainee> selectTrainee(Long id) {
        return traineeService.select(id);
    }

    public List<Trainee> selectAllTrainees() {
        return traineeService.selectAll();
    }


    public Trainer createTrainer(Trainer trainer) {
        log.info("Facade: createTrainer");
        return trainerService.create(trainer);
    }

    public Trainer updateTrainer(Trainer trainer) {
        log.info("Facade: updateTrainer id={}", trainer == null ? null : trainer.getUserId());
        return trainerService.update(trainer);
    }

    public Optional<Trainer> selectTrainer(Long id) {
        return trainerService.select(id);
    }

    public List<Trainer> selectAllTrainers() {
        return trainerService.selectAll();
    }



    public Training createTraining(Training training) {
        log.info("Facade: createTraining");
        return trainingService.create(training);
    }

    public Optional<Training> selectTraining(Long id) {
        return trainingService.select(id);
    }

    public List<Training> selectAllTrainings() {
        return trainingService.selectAll();
    }
}