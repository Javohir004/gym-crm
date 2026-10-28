package com.epam.training.gym.service;

import com.epam.training.gym.dao.TraineeDao;
import com.epam.training.gym.dao.TrainerDao;
import com.epam.training.gym.dao.TrainingDao;
import com.epam.training.gym.exception.NotFoundException;
import com.epam.training.gym.model.Trainee;
import com.epam.training.gym.model.Trainer;
import com.epam.training.gym.model.Training;
import com.epam.training.gym.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@Transactional
public class TrainingService {

    private static final Logger log = LoggerFactory.getLogger(TrainingService.class);

    private TrainingDao trainingDao;
    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTrainingDao(TrainingDao trainingDao) {
        this.trainingDao = trainingDao;
    }

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    public Training addTraining(String traineeUsername, String trainerUsername,
                                String trainingName, LocalDate trainingDate, Integer durationMinutes) {
        Validation.requireText(traineeUsername, "Trainee username");
        Validation.requireText(trainerUsername, "Trainer username");
        Validation.requireText(trainingName, "Training name");
        Validation.requireNotNull(trainingDate, "Training date");
        Validation.requirePositive(durationMinutes, "Training duration");

        Trainee trainee = traineeDao.findByUsername(traineeUsername)
                .orElseThrow(() -> new NotFoundException("Trainee not found: " + traineeUsername));
        Trainer trainer = trainerDao.findByUsername(trainerUsername)
                .orElseThrow(() -> new NotFoundException("Trainer not found: " + trainerUsername));

        Training training = Training.builder()
                .trainee(trainee)
                .trainer(trainer)
                .trainingName(trainingName.trim())
                .trainingType(trainer.getSpecialization())
                .trainingDate(trainingDate)
                .trainingDuration(durationMinutes)
                .build();

        Training saved = trainingDao.save(training);
        log.info("Training added: name='{}' trainee={} trainer={} type={} date={} duration={}min",
                saved.getTrainingName(), traineeUsername, trainerUsername,
                trainer.getSpecialization().getTrainingTypeName(), saved.getTrainingDate(), saved.getTrainingDuration());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Training> getTraineeTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String trainerName, String trainingTypeName) {
        requireTrainee(username);
        log.debug("Searching trainings of trainee username={} from={} to={} trainer='{}' type='{}'",
                username, fromDate, toDate, trainerName, trainingTypeName);
        return trainingDao.findByTraineeUsername(username, fromDate, toDate, trainerName, trainingTypeName);
    }

    @Transactional(readOnly = true)
    public List<Training> getTrainerTrainings(String username, LocalDate fromDate,
                                              LocalDate toDate, String traineeName) {
        requireTrainer(username);
        log.debug("Searching trainings of trainer username={} from={} to={} trainee='{}'",
                username, fromDate, toDate, traineeName);
        return trainingDao.findByTrainerUsername(username, fromDate, toDate, traineeName);
    }

    private void requireTrainee(String username) {
        traineeDao.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainee not found: " + username));
    }

    private void requireTrainer(String username) {
        trainerDao.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Trainer not found: " + username));
    }
}